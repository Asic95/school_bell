# Аналіз механізму оновлення програмного забезпечення

Цей документ містить детальний огляд коду перевірки, завантаження та встановлення оновлень, реалізованого у файлах `MainApp.java`, `UpdateService.java` та `UpdateAvailableDialog.java`.

---

## 1. Резюме
Механізм оновлення загалом спроектований правильно: він делегує мережеві операції у фонові потоки за допомогою `CompletableFuture` (що запобігає зависанню інтерфейсу JavaFX під час запуску), використовує сучасний Java HTTP Client API та коректно планує періодичні перевірки.

Проте було виявлено кілька **критичних проблем**, які стосуються стабільності мережі, безпеки файлової системи, продуктивності JavaFX та взаємодії з користувачем (UX). Їх усунення дозволить зробити процес оновлення надійним та професійним.

---

## 2. Ключові висновки та детальний аналіз

### Висновок 1: Час запуску перевірки
* **Припущення користувача:** Перевірка виконується через 5 секунд після старту.
* **Реальний код:** Стартова перевірка запускається через **10 секунд**, після чого планується періодична перевірка кожні **24 години**:
  ```java
  // MainApp.java: L212
  scheduler.schedule(this::checkForUpdates, 10, TimeUnit.SECONDS);
  scheduler.scheduleAtFixedRate(this::checkForUpdates, 24, 24, TimeUnit.HOURS);
  ```
  * **Рекомендація:** 10 секунд — непоганий вибір, щоб не перевантажувати старт програми. Проте, оскільки діалогове вікно оновлення є **модальним**, його раптова поява через 10 секунд після запуску може перервати роботу користувача, який вже почав щось вводити. Варто подумати про менш нав'язливий спосіб (наприклад, ненав'язливий банер або toast-сповіщення).

---

### Висновок 2: Відсутність таймаутів для HTTP-з'єднань та запитів
* **Код:**
  ```java
  private static final HttpClient httpClient = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
  ```
  І під час створення запиту:
  ```java
  HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).GET().build();
  ```
* **Проблема:** Ні `HttpClient`, ні `HttpRequest` не визначають обмежень за часом (timeouts). Якщо у користувача поганий зв'язок, він перебуває за мережевим екраном (captive portal) або GitHub тимчасово недоступний, запит може зависнути назавжди. Це заблокує потік планувальника і триматиме задачу оновлення активною нескінченно.
* **Рекомендація:** Налаштувати таймаут з'єднання для клієнта та таймаут для самого запиту:
  ```java
  private static final HttpClient httpClient = HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  // При побудові запиту:
  HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(MANIFEST_URL))
          .timeout(Duration.ofSeconds(15))
          .GET()
          .build();
  ```

---

### Висновок 3: Накладання кількох діалогових вікон (Stacking)
* **Код:**
  ```java
  private void checkForUpdates() {
      updateService.checkForUpdates().thenAccept(manifest -> {
          if (manifest != null) {
              Platform.runLater(() -> {
                  new UpdateAvailableDialog(primaryStage, updateService, manifest).show();
              });
          }
      });
  }
  ```
* **Проблема:** Метод `show()` є неблокуючим. Якщо програма працює без перезапуску кілька днів (що характерно для серверів шкільних дзвінків), кожен наступний 24-годинний цикл створюватиме та накладатиме нове вікно `UpdateAvailableDialog` поверх попереднього, якщо користувач не закрив перше.
* **Рекомендація:** Зберігати посилання на активне вікно і перевіряти, чи воно вже відображається на екрані:
  ```java
  private UpdateAvailableDialog activeUpdateDialog;

  private void checkForUpdates() {
      Platform.runLater(() -> {
          if (activeUpdateDialog != null && activeUpdateDialog.isShowing()) {
              return; // Вже відображається
          }
          updateService.checkForUpdates().thenAccept(manifest -> {
              if (manifest != null) {
                  Platform.runLater(() -> {
                      if (activeUpdateDialog == null || !activeUpdateDialog.isShowing()) {
                          activeUpdateDialog = new UpdateAvailableDialog(primaryStage, updateService, manifest);
                          activeUpdateDialog.show();
                      }
                  });
              }
          });
      });
  }
  ```

---

### Висновок 4: Перевантаження черги JavaFX (Зависання/Лаги UI)
* **Код:**
  ```java
  while ((bytesRead = is.read(buffer)) != -1) {
      os.write(buffer, 0, bytesRead);
      downloadedBytes += bytesRead;
      if (totalBytes > 0) {
          progressConsumer.accept((double) downloadedBytes / totalBytes);
      }
  }
  ```
  Де `progressConsumer` викликає:
  ```java
  Platform.runLater(() -> {
      progressBar.setProgress(progress);
      statusLabel.setText(String.format("Завантаження: %.0f%%", progress * 100));
  });
  ```
* **Проблема:** Розмір буфера становить 8 КБ. Для оновлення розміром 40 МБ цей цикл виконається приблизно **5 000 разів**. Це означає, що `Platform.runLater()` викликається 5 000 разів підряд, перевантажуючи чергу головного потоку JavaFX. Це спричинить сильне смикання, лаги або повне «завмирання» інтерфейсу під час завантаження.
* **Рекомендація:** Дроселювати (обмежувати) частоту оновлення UI. Наприклад, надсилати оновлення лише тоді, коли відсоток завантаження змінився хоча б на 1%, або обмежувати частоту за часом (не частіше ніж раз на 100 мс):
  ```java
  int lastPercent = -1;
  while ((bytesRead = is.read(buffer)) != -1) {
      os.write(buffer, 0, bytesRead);
      downloadedBytes += bytesRead;
      if (totalBytes > 0) {
          int percent = (int) ((double) downloadedBytes / totalBytes * 100);
          if (percent > lastPercent) {
              lastPercent = percent;
              progressConsumer.accept((double) percent / 100);
          }
      }
  }
  ```

---

### Висновок 5: Витік тимчасових файлів при помилках
* **Код:**
  ```java
  Path tempFile = Files.createTempFile("schoolbell_update_", ".exe");
  try (InputStream is = response.body();
       OutputStream os = new FileOutputStream(tempFile.toFile())) {
      ...
  }
  ```
* **Проблема:** Тимчасовий файл створюється *до* відкриття потоків введення/виведення. Якщо під час завантаження виникне помилка мережі або переповнення диска всередині блоку `try`, буде викинуто виняток, але тимчасовий файл так і залишиться на диску. Невдалі спроби оновлення будуть поступово засмічувати системну папку Temp великими інсталяційними файлами.
* **Рекомендація:** Обернути роботу з файлом у блок `try-catch`, який видалятиме частково завантажений файл у разі помилки:
  ```java
  Path tempFile = null;
  try {
      tempFile = Files.createTempFile("schoolbell_update_", ".exe");
      try (InputStream is = response.body();
           OutputStream os = Files.newOutputStream(tempFile)) {
           // ... цикл завантаження ...
      }
      // Перевірка контрольної суми
      ...
      return tempFile.toFile();
  } catch (Exception e) {
      if (tempFile != null) {
          try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
      }
      throw new RuntimeException(e);
  }
  ```

---

### Висновок 6: Залежність від заголовка Content-Length
* **Код:**
  ```java
  long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
  // ...
  if (totalBytes > 0) {
      progressConsumer.accept(...);
  }
  ```
* **Проблема:** Якщо сервер або проксі-сервер не передає заголовок `Content-Length` (наприклад, у разі динамічного стиснення або специфічних налаштувань CDN), змінна `totalBytes` дорівнюватиме `-1`. У такому разі `progressConsumer` взагалі не викликатиметься. Користувач бачитиме індикатор прогресу на рівні 0% та статус "Підготовка до завантаження..." протягом усього процесу, хоча файл насправді успішно завантажуватиметься у фоні.
* **Рекомендація:** Обробляти ситуацію, коли `totalBytes == -1`. Тоді переводити ProgressBar в індетермінований режим (анімація «бігунка») та показувати завантажений обсяг у мегабайтах замість відсотків:
  ```java
  // В UpdateService.java:
  if (totalBytes > 0) {
      progressConsumer.accept((double) downloadedBytes / totalBytes);
  } else {
      // Передаємо негативне значення, що позначає мегабайти (наприклад, -downloadedMB)
      progressConsumer.accept(-(double) downloadedBytes / (1024 * 1024));
  }

  // В UpdateAvailableDialog.java:
  if (progress >= 0) {
      progressBar.setProgress(progress);
      statusLabel.setText(String.format("Завантаження: %.0f%%", progress * 100));
  } else {
      progressBar.setProgress(-1); // Невизначений прогрес (indeterminate)
      statusLabel.setText(String.format("Завантажено: %.1f MB", -progress));
  }
  ```

---

### Висновок 7: Блокування інтерфейсу та неможливість закрити вікно
* **Код:**
  ```java
  saveBtn.setDisable(true);
  cancelBtn.setDisable(true);
  ```
* **Проблема:** Діалог використовує стиль `StageStyle.TRANSPARENT`, через що у нього немає системної рамки ОС та кнопок керування вікном (закрити/згорнути). Оскільки під час завантаження обидві кнопки (Оновити та Скасувати) вимикаються, користувач не має жодного способу закрити це вікно або перервати процес. Якщо завантаження «зависне», єдиним виходом буде примусове завершення всієї програми.
* **Рекомендація:**
  1. Не вимикати кнопку скасування, а перейменовувати її на "СХОВАТИ" (щоб згорнути вікно, поки завантаження триває у фоні).
  2. Реалізувати механізм зупинки завантаження (через прапорець `volatile` або переривання потоку).

---

### Висновок 8: Раптове закриття програми при відхиленні UAC (прав адміністратора)
* **Код:**
  ```java
  ProcessBuilder pb = new ProcessBuilder(installerFile.getAbsolutePath(), ...);
  pb.start();
  System.exit(0);
  ```
* **Проблема:** Запуск інсталятора в Windows зазвичай вимагає підвищення прав (UAC). Якщо користувач у вікні UAC натисне **Ні** (відхилить запит), процес інсталятора завершиться помилкою. Проте Java-додаток до цього моменту вже встигне виконати `System.exit(0)`. Як результат — програма просто закриється, оновлення не встановиться, а користувачу доведеться запускати все вручну.
* **Рекомендація:** Повністю уникнути цієї проблеми на Windows складно (оскільки інсталятор має закрити додаток перед заміною файлів через параметр `/CLOSEAPPLICATIONS`). Проте можна вивести коротке попередження користувачу безпосередньо перед запуском процесу або дати невелику затримку перед виходом з програми.
