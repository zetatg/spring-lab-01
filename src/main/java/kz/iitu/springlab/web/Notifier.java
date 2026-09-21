package kz.iitu.springlab.web;

public interface Notifier {
    String send(String message);
    String channel();
}