package com.github.ventureo.jmessagestransfer.transfers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import com.github.ventureo.jmessagestransfer.Transfer;
import com.github.ventureo.jmessagestransfer.handlers.TelegramHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramTransfer extends TelegramLongPollingBot implements Transfer {
    private final Logger logger = LoggerFactory.getLogger(TelegramTransfer.class);
    private final TelegramHandler handler = new TelegramHandler(this);

    private final Properties config;

    public TelegramTransfer(Properties config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return getConfig().getProperty("telegram.botname");
    }

    @Override
    public String getBotToken() {
        return getConfig().getProperty("telegram.token");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage().hasText()) {
            if (update.getMessage().getText().matches("!id"))
                sendTextMessage(String.valueOf(update.getMessage().getChatId()), "[БОТ]");
        }
        if (!String.valueOf(update.getMessage().getChatId())
                .equals(this.getChatId()))
            return;
        handler.handle(update.getMessage());
    }

    @Override
    public void sendTextMessage(String text, String author) {
        SendMessage answer = new SendMessage();
        answer.setText(author + ": " + text);
        answer.setChatId(this.getChatId());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPhotoMessage(String caption, String url, String name) {
        SendPhoto photo = new SendPhoto();
        InputFile file = new InputFile();
        file.setMedia(url);
        photo.setPhoto(file);
        photo.setChatId(this.getChatId());
        photo.setCaption(name + ": " + caption);
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            sendTextMessage("Не могу переслать фотографию/стикер от " + name + " :("
                    + "\nКод ошибки: " + e.getMessage(), "[БОТ]");
            e.printStackTrace();
        }
    }

    @Override
    public void sendSticker(String url, String name, boolean isVideo) {
        if (isVideo) {
            this.sendVideo("", url, name);
        } else {
            this.sendPhotoMessage("", url, name);
        }
    }

    @Override
    public void sendVideo(String caption, String url, String name) {
        this.sendTextMessage(caption + "\n" + url, name);
    }

    @Override
    public void sendDocument(String caption, String url, String name) {
        this.sendTextMessage(caption + "\n" + url, name);
    }

    @Override
    public void sendAudio(String caption, String url, String name, String title) {
        SendAudio audio = new SendAudio();
        InputFile file = new InputFile();
        try {
            InputStream is = new URL(url).openStream();
            file.setMedia(is, title);
        } catch (IOException e) {
            e.printStackTrace();
        }
        audio.setAudio(file);
        audio.setCaption(name + ": " + caption);
        audio.setTitle(title);
        audio.setChatId(this.getChatId());
        try {
            execute(audio);
        } catch (TelegramApiException e) {
            sendTextMessage("Не могу переслать аудиозапись от " + name + " :("
                    + "\nКод ошибки: " + e.getMessage(), "[БОТ]");
            e.printStackTrace();
        }
    }

    public Properties getConfig() {
        return config;
    }

    public TelegramHandler getHandler() {
        return handler;
    }

    public void start() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(this);
            logger.info("Telegram bot works properly");
        } catch (TelegramApiException e) {
            logger.error("An error occurred during the start of the Telegram bot", e);
        }
    }

    private String getChatId() {
        return getConfig().getProperty("telegram.chatid");
    }
}
