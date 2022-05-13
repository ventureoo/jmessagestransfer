package com.github.ventureo.jmessagestransfer.handlers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import com.github.ventureo.jmessagestransfer.Transfer;
import com.github.ventureo.jmessagestransfer.TransferManager;
import com.github.ventureo.jmessagestransfer.transfers.TelegramTransfer;
import com.luciad.imageio.webp.WebPReadParam;

import org.apache.commons.io.FileUtils;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Video;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramHandler {
    private Transfer transfer;
    private final TelegramTransfer instance;

    public TelegramHandler(TelegramTransfer instance) {
        this.instance = instance;
    }

    public void handle(Message message) {
        if (message.isReply()) {
            Message reply = message.getReplyToMessage();
            User user = reply.getFrom();
            User fakeUser = new User(user.getId(), "| Переслано от: " + user.getFirstName(), user.getIsBot());
            reply.setFrom(fakeUser);
            if (reply.hasText()) {
                reply.setText(reply.getText().replaceAll("\n", "\n| "));
            } else if (reply.getCaption() != null) {
                reply.setCaption(reply.getCaption().replaceAll("\n", "\n| "));
            }
            onMessage(reply);
        }
        onMessage(message);
    }

    private void onMessage(Message message) {
        String name = getTelegramName(message.getFrom()) + " (Telegram)";
        String caption = (message.getCaption() == null) ? "" : message.getCaption();
        String text = message.getText();

        try {
            if (message.hasText()) {
                transfer.sendTextMessage(text, name);
            } else if (message.hasPhoto()) {
                PhotoSize size = message.getPhoto().get(message.getPhoto().size() - 1);
                transfer.sendPhotoMessage(caption,
                        downloadMedia(size.getFileId(), size.getFileId() + ".png"),
                        name);
            } else if (message.hasSticker()
                    && Boolean.parseBoolean(transfer.getConfig().getProperty("stickers.enabled"))) {
                Sticker sticker = message.getSticker();
                if (!message.getSticker().getIsVideo()) {
                    transfer.sendSticker(convertSticker(sticker), name, false);
                } else {
                    transfer.sendSticker(
                            downloadMedia(sticker.getFileId(), sticker.getSetName() + ".mp4"),
                            name, true);
                }
            } else if (message.hasVideo()) {
                Video video = message.getVideo();
                transfer.sendVideo(caption,
                        downloadMedia(video.getFileId(), video.getFileName()),
                        name);
            } else if (message.hasDocument()) {
                Document document = message.getDocument();
                transfer.sendDocument(caption,
                        downloadMedia(document.getFileId(), document.getFileName()),
                        name);
            } else if (message.hasAudio()) {
                Audio audio = message.getAudio();
                transfer.sendAudio(caption,
                        downloadMedia(audio.getFileId(), audio.getFileName()), name, audio.getFileName());
            } else if (message.hasVoice()) {
                Voice voice = message.getVoice();
                transfer.sendAudio(caption,
                        downloadMedia(voice.getFileId(),
                                (name + "- Голосовое сообщение (" + voice.getDuration() + " секунд).oga")),
                        name, "");
            }
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("[400] Bad Request: file is too big")) {
                transfer.sendTextMessage("Не могу переслать файл из Telegram, он слишком большой (>20МБ) :("
                        + "\nПожалуйста, попросите пользователя перезалить его на альтернативный хостинг/платформу.",
                        "[Бот]");
                instance.sendTextMessage("Не могу переслать файл во Вконтакте, он слишком большой (>20МБ) :("
                        + "\nПожалуйста, перезалейте его на альтернативный хостинг/платформу.", "[Бот]");
                return;
            }
            e.printStackTrace();
        }
    }

    private String convertSticker(Sticker sticker) throws TelegramApiException {
        String path = downloadMedia(sticker.getFileId(), sticker.getSetName());
        java.io.File webp = new java.io.File(path);
        java.io.File png = new java.io.File(webp.getAbsolutePath() + ".png");

        // Convert to PNG
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
        WebPReadParam readParam = new WebPReadParam();
        readParam.setBypassFiltering(true);
        readParam.setUseThreads(true);
        readParam.setUseScaling(true);
        readParam.setScaledHeight(Integer.parseInt(transfer.getConfig().getProperty("stickers.size")));
        readParam.setScaledWidth(Integer.parseInt(transfer.getConfig().getProperty("stickers.size")));

        try {
            reader.setInput(new FileImageInputStream(webp));
            // Decode the image
            BufferedImage image = reader.read(0, readParam);
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            e.printStackTrace();
        }

        webp.delete();
        return png.getAbsolutePath();
    }

    private String downloadMedia(String uuid, String name) throws TelegramApiException {
        GetFile getFile = new GetFile();
        getFile.setFileId(uuid);
        File file = instance.execute(getFile);

        String path = TransferManager.PATH + name;

        java.io.File cached = new java.io.File(path);
        try {
            InputStream is = new URL(file.getFileUrl(instance.getBotToken())).openStream();
            FileUtils.copyInputStreamToFile(is, cached);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cached.getAbsolutePath();
    }

    private String getTelegramName(User user) {
        String name = user.getFirstName();

        if (user.getLastName() != null) {
            name = name + " " + user.getLastName();
        }
        return name;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }
}
