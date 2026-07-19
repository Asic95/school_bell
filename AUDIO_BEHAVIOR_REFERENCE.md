# 🎵 Довідник аудіо-поведінки — School Bell

> Цей файл описує **всю логіку роботи аудіо в системі**: fade in/out, затримки між треками, переходи,
> та особливості кожного сценарію відтворення. Основні файли: `AudioService.java`, `MediaSchedulerService.java`, `SignalService.java`.

---

## 📁 Задіяні файли

| Файл | Роль |
| :--- | :--- |
| `src/.../service/AudioService.java` | **Серце аудіо-рушія.** PCM-відтворення, fade in/out, software volume. |
| `src/.../service/MediaSchedulerService.java` | Планування аудіо (перерви, час, цикли). Затримки між треками. |
| `src/.../service/SignalService.java` | Тригери тривог та дзвінків. Передає команди до AudioService. |

---

## 🔊 1. AudioService — рушій відтворення

### 1.1 Загальні параметри

```java
private static final int FADE_DURATION_MS = 2500; // 2.5 секунди
```

- **Один глобальний `sessionId`** — кожен новий запуск аудіо інкрементує `globalSessionId`.
  Старі потоки автоматично зупиняються, коли бачать, що їх `sessionId` вже не актуальний.
- **Формат PCM**: `44100 Hz, 16-bit, Stereo, Little Endian` — стандарт для всіх треків.
- **Буфер читання**: `8192 байт` за одну ітерацію.

---

### 1.2 Fade-in (початок треку)

**Де:** метод `playInternal()` в `AudioService.java`.

```java
long fadeFrames = (long) (format.getFrameRate() * (FADE_DURATION_MS / 1000.0));
if (!isAlert && frameCounter < fadeFrames) {
    volumeMultiplier *= (float) frameCounter / fadeFrames;
}
```

**Як працює:**
- Тривалість fade-in: **2.5 секунди** (константа `FADE_DURATION_MS`).
- Гучність поступово росте від `0.0` до `targetSystemVolume` протягом перших 2.5 секунд трека.
- **Тривожні звуки (`isAlert = true`) fade-in НЕ мають** — грають одразу на повній гучності.
  - Трек вважається "alert", якщо його шлях містить слово `error` або `alert`.

---

### 1.3 Fade-out (завершення / зупинка)

**Де:** метод `fadeOutAndStop()` в `AudioService.java`.

```java
for (long f = fadeOutFrames; f > 0; f -= (buffer.length / format.getFrameSize())) {
    float fadeMultiplier = (float) f / fadeOutFrames; // від 1.0 до 0.0
    applySoftwareVolume(buffer, bytesRead, targetVol * fadeMultiplier);
    line.write(buffer, 0, bytesRead);
}
```

**Як працює:**
- Тривалість fade-out: **2.5 секунди**.
- Fade-out запускається у двох випадках:
  1. `stopAll()` встановлює `isStopping = true` — наступна ітерація читання починає fade-out.
  2. **Перехід між треками** — старий трек дограє fade-out у фоні, поки новий вже готується.
- **Тривожні звуки fade-out НЕ мають** — перевірка `if (isStopping && !isAlert)`.

---

### 1.4 Режими зупинки

| Метод | Поведінка | Використовується |
| :--- | :--- | :--- |
| `stopAll()` | Встановлює `isStopping = true`. Fade-out 2.5 с, потім трек сам зупиняється. | MediaSchedulerService — перед дзвінком або переходом |
| `stopAllAndWait()` | Те ж саме, але **блокує потік** на час fade (2.5 с + 750 мс). | Резервний варіант |
| `stopImmediate()` | Інкрементує `sessionId`, **різко** закриває `SourceDataLine`. 20 мс затримка для стабільності. | Тривоги (AIR_RAID, EMERGENCY) |
| `stopForTransition()` *(приватний)* | Інкрементує `sessionId`, очищує state — але **не чіпає** лінію. Стара лінія закриється сама через fade-out. | `playPlaylist()`, `playStream()` перед новим стартом |

---

### 1.5 Програмне керування гучністю

```java
int sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
int newSample = (int) (sample * volume);
// захист від clipping: обрізка до [-32768, 32767]
```

- Гучність береться з `configService.getSystemVolume() / 100.0f`.
- Якщо `volume >= 0.999f` — пропускає обробку (оптимізація).
- Якщо `volume <= 0.001f` — заповнює буфер нулями (тиша).

---

## ⏱️ 2. MediaSchedulerService — затримки між треками

> **Тут живе логіка 1.5-секундної паузи між музикою.**

### 2.1 Метод `playEvent()` — повна логіка затримок

```java
// Крок 1: Чекаємо завершення дзвінка
while (mainApp.getSignalService().isActionInProgress()) {
    hadBell = true;
    Thread.sleep(500);
}

// Крок 2: Grace period після дзвінка — 3 секунди
if (hadBell) {
    Thread.sleep(3000);
}

// Крок 3а: Якщо зараз щось грає — fade-out + пауза
if (audioService.getCurrentPlayingTrack() != null) {
    audioService.stopAll();       // запускаємо fade-out (2.5 с)
    Thread.sleep(4000);           // 2.5 с fade-out + 1.5 с тиша = 4.0 с загалом
}
// Крок 3б: Якщо тиша — перевіряємо мінімальну паузу
else {
    long elapsed = System.currentTimeMillis() - lastAudioFinishedTimeMs;
    long requiredPauseMs = 1500;  // мінімум 1.5 секунди тиші
    if (elapsed < requiredPauseMs) {
        Thread.sleep(requiredPauseMs - elapsed);
    }
}
```

### 2.2 Таблиця затримок

| Ситуація | Затримка | Пояснення |
| :--- | :--- | :--- |
| Перехід, якщо щось грає | **4.0 секунди** | 2.5 с fade-out + 1.5 с тиша |
| Перехід після природного кінця треку | **0–1.5 секунд** | Добирає до мінімуму 1.5 с тиші |
| Старт після дзвінка | **3 секунди** | Grace period після bell |
| Старт після тривоги | **негайно** | `stopImmediate()` без fade |

### 2.3 Чому є 1.5-секундна пауза

**Це навмисна поведінка**, а не баг. Вона забезпечує:
1. **Чітку паузу між треками** — музика не "склеюється" без зупинки.
2. **Природне сприйняття** — немає відчуття, що щось "підхопилося" одразу.
3. **Запас після fade-out** — 2.5 с fade + 1.5 с = вухо чітко відчуває тишу.

---

### 2.4 Зупинка перед дзвінком

Музика зупиняється **завчасно**, до того як задзвонить дзвінок:

```java
if (totalEarlyOffsetSeconds > 0) {
    // З завчасним дзвінком: зупинитись ще раніше
    secondsToStop = minSecondsToBell - totalEarlyOffsetSeconds - 3;
} else {
    // Стандарт: за 10 секунд до дзвінка
    secondsToStop = minSecondsToBell - 10;
}
```

| Режим | Коли музика зупиняється |
| :--- | :--- |
| Без завчасного дзвінка | За **10 секунд** до дзвінка |
| З завчасним дзвінком | За `earlyOffset + 3` секунди до дзвінка |

Після цього: `stopAll()` → fade-out 2.5 с.

---

## 🚨 3. SignalService — тривоги

### 3.1 Пауза між реле та аудіо тривоги

У всіх тривожних сценаріях між закінченням реле і стартом аудіо є затримка:

```java
Thread.sleep(1500); // 1.5 секунди
audioService.playAudioFile(resolveAudioPath(...));
```

| Сигнал | Пауза перед аудіо |
| :--- | :--- |
| AIR_RAID (тривога) | **1.5 секунди** після останнього імпульсу реле |
| AIR_RAID (відбій) | **1.5 секунди** |
| EMERGENCY (НС) | **1.5 секунди** |

### 3.2 Особливості тривожних аудіо

- **НЕ мають fade-in** — грають одразу на повній гучності.
- **НЕ мають fade-out** при `isStopping`.
- Перервати можна тільки через `stopImmediate()`.

---

## 🔄 4. Загальна схема переходів

```
[Трек А грає]
      │
      ▼
 stopAll() → isStopping = true
      │
 [fade-out 2.5 с у фоні]
      │
 Thread.sleep(4000) ─── fade(2.5) + пауза(1.5) = 4.0 с
      │
      ▼
 [Трек Б стартує]
 [fade-in 2.5 с: 0.0 → maxVolume]
      │
 ... музика грає ...
      │
 [за 10 с до дзвінка: stopAll()]
 [fade-out 2.5 с]
      │
 [Дзвінок]
```

---

## 📌 5. Константи для швидкого пошуку

| Константа / Значення | Файл | Рядок | Призначення |
| :--- | :--- | :--- | :--- |
| `FADE_DURATION_MS = 2500` | `AudioService.java` | ~16 | Тривалість fade-in та fade-out (мс) |
| `Thread.sleep(4000)` | `MediaSchedulerService.java` | ~247 | Пауза між треками: 2.5 fade + 1.5 тиша |
| `requiredPauseMs = 1500` | `MediaSchedulerService.java` | ~254 | Мінімальна тиша після природного кінця |
| `Thread.sleep(3000)` | `MediaSchedulerService.java` | ~237 | Grace period після дзвінка |
| `Thread.sleep(1500)` | `SignalService.java` | ~114, 143, 171 | Пауза між реле і аудіо тривоги |
| `secondsToStop = ... - 10` | `MediaSchedulerService.java` | ~202 | Зупинка музики за 10 с до дзвінка |
| `buffer = new byte[8192]` | `AudioService.java` | ~193 | Розмір PCM-буфера читання |
