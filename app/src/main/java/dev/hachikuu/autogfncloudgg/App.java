package dev.hachikuu.autogfncloudgg;

public class App {
    public static void main(String[] args) {
        Thread appThread = new Thread(new AppThread());
        appThread.run();
    }
}
