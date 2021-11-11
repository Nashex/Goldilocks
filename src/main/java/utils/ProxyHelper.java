package utils;

import main.Config;
import main.Goldilocks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import verification.VerificationHub;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ProxyHelper {

    public static LinkedHashMap<String, Integer> proxyList = new LinkedHashMap<>();
    public Map.Entry<String, Integer> currentProxy;
    private int proxyIndex = 0;
    private final long switchIpInterval = 360L; //Seconds
    private final long rotateListInterval = 30L; //Minutes

    public ProxyHelper() {
        refreshProxyList();
        currentProxy = (Map.Entry<String, Integer>) proxyList.entrySet().toArray()[proxyIndex];

        //Refresh proxy list on a scheduled basis
        Goldilocks.TIMER.scheduleWithFixedDelay(() -> {
            Map.Entry<String, Integer> temp = (Map.Entry<String, Integer>) proxyList.entrySet().toArray()[proxyIndex];
            refreshProxyList();
            if (!temp.equals((Map.Entry<String, Integer>) proxyList.entrySet().toArray()[proxyIndex])) proxyIndex = 0;
        }, 10L, rotateListInterval, TimeUnit.MINUTES);

        //Change proxy on a scheduled basis
        Goldilocks.TIMER.scheduleWithFixedDelay(this::nextProxy, 2L, switchIpInterval, TimeUnit.SECONDS);
    }

    public void refreshProxyList() {
        LinkedHashMap<String, Integer> proxyList = new LinkedHashMap<>();
        try {
            Document doc = Jsoup.connect("https://proxy.webshare.io/api/proxy/list/?page=0&countries=US").header("Authorization" , "Token " + Config.get("PROXY_TOKEN"))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36")
                    .ignoreContentType(true)
                    .get();

            String json = doc.body().text();
            JSONObject jsonObject = new JSONObject(json);
            System.out.print("Refreshed Proxy List: ");

            JSONArray arr = jsonObject.getJSONArray("results");
            for (int i = 0; i < arr.length(); i++) {
                String proxyIp = arr.getJSONObject(i).getString("proxy_address");
                int proxyPort = arr.getJSONObject(i).getJSONObject("ports").getInt("http");

                proxyList.put(proxyIp, proxyPort);
                System.out.print("{Proxy#" + i + ": " + proxyIp + ":" + proxyPort + "} ");
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("Failed to retrieve IP list");
        }
        if (!proxyList.isEmpty()) ProxyHelper.proxyList = proxyList;
    }

    public void nextProxy() {
        if (proxyList.isEmpty()) refreshProxyList();
        Goldilocks.TIMER.schedule(() -> {
            boolean banned = true;
            int timesTried = 0;
            while (banned) {
                if (timesTried > 1) refreshProxyList();
                if (proxyIndex + 1 >= proxyList.size()) proxyIndex = 0;
                currentProxy = (Map.Entry<String, Integer>) proxyList.entrySet().toArray()[++proxyIndex];
                try {
                    Jsoup.connect("https://realmeye.com")
                            .headers(VerificationHub.headers)
                            .proxy(currentProxy.getKey(), currentProxy.getValue())
                            .ignoreContentType(true)
//                        .timeout(3000)
                            .get();
                    banned = false;
                    System.out.println("Found Valid Proxy: " + currentProxy.getKey() + ":" + currentProxy.getValue());
                } catch (Exception e) {
                    System.out.println("Invalid Proxy: " + currentProxy.getKey() + ":" + currentProxy.getValue()); timesTried++;}
            }

        }, 0L, TimeUnit.SECONDS);
        System.out.println();
    }

    public static Map.Entry<String, Integer> randomProxy(LinkedHashMap<String, Integer> proxyList) {
        if (ProxyHelper.proxyList.isEmpty()) Goldilocks.proxyHelper.refreshProxyList();
        return (Map.Entry<String, Integer>) proxyList.entrySet().toArray()[new Random().nextInt(proxyList.size() - 1)];
    }
}


