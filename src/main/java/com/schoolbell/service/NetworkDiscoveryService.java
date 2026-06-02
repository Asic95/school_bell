package com.schoolbell.service;

import com.schoolbell.MainApp;
import com.schoolbell.hardware.ShellyRelayDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkDiscoveryService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkDiscoveryService.class);
    
    private final MainApp mainApp;
    private final List<ShellyRelayDevice> discoveredDevices = new CopyOnWriteArrayList<>();
    private JmDNS jmdns;

    public NetworkDiscoveryService(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void startScan(long durationMs, Runnable onFinish) {
        new Thread(() -> {
            try {
                discoveredDevices.clear();
                jmdns = JmDNS.create(InetAddress.getLocalHost());
                
                ServiceListener listener = new ServiceListener() {
                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        jmdns.requestServiceInfo(event.getType(), event.getName());
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {}

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        String name = event.getName().toLowerCase();
                        if (name.contains("shelly")) {
                            String ip = event.getInfo().getHostAddresses()[0];
                            if (!isAlreadyDiscovered(ip)) {
                                discoveredDevices.add(new ShellyRelayDevice(mainApp, ip, event.getName()));
                                logger.info("Discovered Shelly: {} at {}", event.getName(), ip);
                            }
                        }
                    }
                };

                jmdns.addServiceListener("_http._tcp.local.", listener);
                Thread.sleep(durationMs);
                jmdns.removeServiceListener("_http._tcp.local.", listener);
                jmdns.close();
                
            } catch (Exception e) {
                logger.error("Network discovery error: {}", e.getMessage());
            } finally {
                if (onFinish != null) onFinish.run();
            }
        }, "Shelly-Discovery-Thread").start();
    }

    private boolean isAlreadyDiscovered(String ip) {
        return discoveredDevices.stream().anyMatch(d -> d.getIp().equals(ip));
    }

    public List<ShellyRelayDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
}
