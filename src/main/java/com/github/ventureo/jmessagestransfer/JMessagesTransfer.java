package com.github.ventureo.jmessagestransfer;

public class JMessagesTransfer {

    public static void main(String[] args) {
        TransferManager manager = new TransferManager();
        try {
            manager.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
