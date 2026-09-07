package com.mapsyncer.debug;

import com.mapsyncer.gtceu.GtceuVeinBridge;
import com.mapsyncer.network.payload.OreVeinSyncPayload;
import com.mapsyncer.MapSyncer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.Executors;

/** Local read-only diagnostic map endpoint; it mirrors the records the sync layer can capture. */
public final class GtceuDebugWebServer {
    private static HttpServer server;
    private static volatile MinecraftServer minecraftServer;
    private static volatile List<OreVeinSyncPayload.OreVeinSnapshot> lastCaptured = Collections.emptyList();
    private static volatile long lastCaptureMs;
    private GtceuDebugWebServer() {}
    public static synchronized void start(MinecraftServer mcServer) {
        if (server != null) return;
        minecraftServer = mcServer;
        try {
            int port = Integer.getInteger("mapsyncer.debugWebPort", 8765);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/api/veins", GtceuDebugWebServer::veins);
            server.createContext("/api/status", GtceuDebugWebServer::status);
            server.createContext("/", GtceuDebugWebServer::index);
            server.setExecutor(Executors.newCachedThreadPool(r -> { Thread t=new Thread(r,"mapsyncer-debug-web"); t.setDaemon(true); return t; }));
            server.start();
            MapSyncer.LOGGER.info("GTCEu debug web map listening at http://127.0.0.1:{}/", port);
        } catch (IOException e) { MapSyncer.LOGGER.warn("Could not start GTCEu debug web map: {}", e.toString()); server=null; }
    }
    private static void veins(HttpExchange exchange) throws IOException {
        List<OreVeinSyncPayload.OreVeinSnapshot> veins = lastCaptured;
        byte[] data=toJson(veins).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8"); exchange.getResponseHeaders().set("Access-Control-Allow-Origin","*"); exchange.sendResponseHeaders(200,data.length); try(OutputStream o=exchange.getResponseBody()){o.write(data);}
    }
    private static String toJson(List<OreVeinSyncPayload.OreVeinSnapshot> veins) { StringBuilder b=new StringBuilder("["); for(int i=0;i<veins.size();i++){ if(i>0)b.append(','); var v=veins.get(i); b.append("{\"id\":\"").append(json(v.id())).append("\",\"dimension\":\"").append(json(v.dimension())).append("\",\"centerX\":").append(v.centerX()).append(",\"centerY\":").append(v.centerY()).append(",\"centerZ\":").append(v.centerZ()).append(",\"depleted\":").append(v.depleted()).append('}'); } return b.append(']').toString(); }
    private static String json(String s) { return s==null ? "" : s.replace("\\","\\\\").replace("\"","\\\""); }
    public static synchronized void publishBatch(OreVeinSyncPayload payload) {
        if (payload.batchIndex() == 0) lastCaptured = new java.util.ArrayList<>();
        java.util.ArrayList<OreVeinSyncPayload.OreVeinSnapshot> merged = new java.util.ArrayList<>(lastCaptured);
        merged.addAll(payload.veins());
        lastCaptured = List.copyOf(merged);
        lastCaptureMs = System.currentTimeMillis();
    }
    private static void status(HttpExchange exchange) throws IOException {
        byte[] data=("{\"available\":"+GtceuVeinBridge.isAvailable()+",\"captured\":"+lastCaptured.size()+",\"ageMs\":"+(System.currentTimeMillis()-lastCaptureMs)+"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8"); exchange.sendResponseHeaders(200,data.length); try(OutputStream o=exchange.getResponseBody()){o.write(data);}
    }
    private static void index(HttpExchange exchange) throws IOException {
        byte[] data=WEB.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8"); exchange.sendResponseHeaders(200,data.length); try(OutputStream o=exchange.getResponseBody()){o.write(data);}
    }
    public static synchronized void stop(){ if(server!=null){server.stop(0);server=null;} minecraftServer=null; lastCaptured=Collections.emptyList(); lastCaptureMs=0; }
    private static final String WEB = "<!doctype html><meta charset=\"utf-8\"><title>MapSyncer GTCEu Veins</title><style>body{margin:0;background:#111;color:#ddd;font:14px sans-serif}#map{height:100vh;background:radial-gradient(#344 1px,transparent 1px);background-size:24px 24px;position:relative;overflow:hidden}.v{position:absolute;width:9px;height:9px;background:#e85;border-radius:50%;box-shadow:0 0 8px #f80}.panel{position:fixed;top:10px;left:10px;background:#222d;padding:10px;border-radius:6px}</style><div class=panel id=s>Loading...</div><div id=map></div><script>async function load(){let v=await (await fetch(\"/api/veins\")).json(),m=document.querySelector(\"#map\");if(!v.length){s.textContent=\"No captured veins\";return}let xs=v.map(x=>x.centerX),zs=v.map(x=>x.centerZ),minx=Math.min(...xs),maxx=Math.max(...xs),minz=Math.min(...zs),maxz=Math.max(...zs);v.forEach(x=>{let e=document.createElement(\"i\");e.className=\"v\";e.title=x.id+\" \"+x.dimension+\" [\"+x.centerX+\", \"+x.centerY+\", \"+x.centerZ+\"]\";e.style.left=((x.centerX-minx)/(maxx-minx||1)*96+2)+\"%\";e.style.top=((x.centerZ-minz)/(maxz-minz||1)*96+2)+\"%\";m.append(e)});s.textContent=v.length+\" veins | \"+[...new Set(v.map(x=>x.dimension))].join(\", \" )}load();setInterval(load,5000)</script>";
}