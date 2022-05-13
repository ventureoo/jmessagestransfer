package com.github.ventureo.jmessagestransfer.transfers;

import java.io.File;
import java.util.Properties;

import com.github.ventureo.jmessagestransfer.Transfer;
import com.github.ventureo.jmessagestransfer.handlers.VkHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.longpoll.bots.LongPollBot;
import api.longpoll.bots.exceptions.VkApiException;
import api.longpoll.bots.model.events.messages.MessageNew;

public class VkTransfer extends LongPollBot implements Transfer {
    private final Logger logger = LoggerFactory.getLogger(VkTransfer.class);
    private final VkHandler handler = new VkHandler(vk);

    private final Properties config;

    public VkTransfer(Properties config) {
        this.config = config;
    }

    @Override
    public String getAccessToken() {
        return getConfig().getProperty("vk.apikey");
    }

    @Override
    public void onMessageNew(MessageNew messageNew) {
        if (messageNew.getMessage().hasText()) {
            if (messageNew.getMessage().getText().matches("!id"))
                sendTextMessage(String.valueOf(messageNew.getMessage().getPeerId()), "[БОТ]");
        }
        if (messageNew.getMessage().getPeerId() != this.getChatId())
            return;
        try {
            handler.handle(messageNew.getMessage());
        } catch (VkApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendTextMessage(String text, String author) {
        try {
            vk.messages.send()
                    .setMessage(author + ": " + text)
                    .setPeerId(getChatId())
                    .execute();
        } catch (VkApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPhotoMessage(String caption, String url, String name) {
        File cached = new File(url);
        try {
            vk.messages.send()
                    .setPeerId(getChatId())
                    .setMessage(name + ": " + caption)
                    .addPhoto(cached)
                    .execute();
        } catch (VkApiException e) {
            e.printStackTrace();
        }
        cached.delete();
    }

    @Override
    public void sendSticker(String url, String name, boolean isVideo) {
        if (isVideo) {
            this.sendDocument("", url, name);
        } else {
            this.sendPhotoMessage("", url, name);
        }
    }

    @Override
    public void sendVideo(String caption, String url, String name) {
        this.sendDocument(caption, url, name);
    }

    @Override
    public void sendDocument(String caption, String url, String name) {
        File doc = new File(url);
        try {
            vk.messages.send()
                    .setMessage(name + ": " + caption)
                    .setPeerId(getChatId())
                    .addDoc(doc)
                    .execute();
        } catch (VkApiException e) {
            // Workaround for sending files with an unacceptable extension to VK
            if (e.getMessage().contains("no extension found")
                    || (e.getMessage().contains("file") && e.getMessage().contains("wrong"))
                            && !doc.getName().endsWith(".telegram")) {
                File renamedDoc = new File(url + ".telegram");
                if (doc.renameTo(renamedDoc)) {
                    doc.delete();
                    caption = caption + "(не обращайте внимания на расширение):";
                    sendDocument(caption, renamedDoc.getPath(), name);
                    return;
                }
            }
            e.printStackTrace();
        }
        doc.delete();
    }

    @Override
    public void sendAudio(String caption, String url, String name, String title) {
        sendDocument(caption + "\nАудиозапись: " + title, url, name);
    }

    public VkHandler getHandler() {
        return handler;
    }

    public Properties getConfig() {
        return config;
    }

    public void start() {
        try {
            logger.info("VK bot registered fine");
            this.startPolling();
        } catch (VkApiException e) {
            logger.info("An error occurred during the start of the VK bot", e);
        }
    }

    private int getChatId() {
        return Integer.parseInt(config.getProperty("vk.chatid"));
    }
}
