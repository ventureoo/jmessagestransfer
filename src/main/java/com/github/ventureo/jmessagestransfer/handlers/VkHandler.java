package com.github.ventureo.jmessagestransfer.handlers;

import com.github.ventureo.jmessagestransfer.Transfer;

import java.util.List;
import api.longpoll.bots.exceptions.VkApiException;
import api.longpoll.bots.methods.VkBotsMethods;
import api.longpoll.bots.model.objects.additional.PhotoSize;
import api.longpoll.bots.model.objects.basic.Community;
import api.longpoll.bots.model.objects.basic.Message;
import api.longpoll.bots.model.objects.basic.User;
import api.longpoll.bots.model.objects.basic.WallPost;
import api.longpoll.bots.model.objects.media.AttachedLink;
import api.longpoll.bots.model.objects.media.Attachment;
import api.longpoll.bots.model.objects.media.AttachmentObject;
import api.longpoll.bots.model.objects.media.Audio;
import api.longpoll.bots.model.objects.media.AudioMessage;
import api.longpoll.bots.model.objects.media.Doc;
import api.longpoll.bots.model.objects.media.Photo;
import api.longpoll.bots.model.objects.media.Sticker;
import api.longpoll.bots.model.objects.media.Video;

public class VkHandler {
    private Transfer transfer;
    private final VkBotsMethods vk;

    public VkHandler(VkBotsMethods vk) {
        this.vk = vk;
    }

    public void handle(Message message) throws VkApiException {
        if (message.hasFwdMessages()) {
            message.getFwdMessages().forEach(reply -> {
                try {
                    onMessage(reply, true);
                } catch (VkApiException e) {
                    e.printStackTrace();
                }
            });
        }
        onMessage(message, false);
    }

    private void onMessage(Message message, boolean isReply) throws VkApiException {
        String name = getVKName(message.getFromId());
        String text = message.getText() == null ? "" : message.getText();

        if (isReply) {
            name = "| Переслано от " + name;
            text = text.replaceAll("\n", "\n| ");
        }

        onTextMessage(text, name, message.getAttachments());
    }

    public void onTextMessage(String text, String name, List<Attachment> attachments) {
        if (attachments.isEmpty()) {
            transfer.sendTextMessage(text, name);
        } else {
            attachments.forEach(attachment -> {
                try {
                    onAttachmentMessage(
                            attachment.getAttachmentObject(),
                            name, text);
                } catch (VkApiException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // TODO: The method is too big. Should we break it down into smaller ones?
    private void onAttachmentMessage(AttachmentObject object, String name, String caption) throws VkApiException {
        if (object instanceof Photo) {
            Photo photo = (Photo) object;
            int maxX = 0;
            int maxY = 0;
            String url = "";
            for (PhotoSize photoSize : photo.getPhotoSizes()) {
                if (maxX < photoSize.getWidth() && maxY < photoSize.getHeight()) {
                    maxX = photoSize.getWidth();
                    maxY = photoSize.getHeight();
                    url = photoSize.getSrc();
                }
            }
            transfer.sendPhotoMessage(caption, url, name);
        } else if (object instanceof Video) {
            Video video = (Video) object;
            transfer.sendVideo(caption,
                    "https://vk.com/video" + video.getOwnerId() + "_" + video.getId(), name);
        } else if (object instanceof Doc) {
            Doc doc = (Doc) object;
            transfer.sendDocument(caption + "\nФайл: " + doc.getTitle(), doc.getUrl(), name);
        } else if (object instanceof Sticker) {
            Sticker sticker = (Sticker) object;
            String url = sticker.getImages().get(sticker.getImages().size() - 1).getUrl();
            transfer.sendSticker(
                    url.replace("-512", "-" + transfer.getConfig().getProperty("stickers.size")),
                    name,
                    false);
        } else if (object instanceof Audio) {
            Audio audio = (Audio) object;
            transfer.sendAudio(caption,
                    audio.getUrl(), name, (audio.getArtist() + " - " + audio.getTitle()));
        } else if (object instanceof AudioMessage) {
            AudioMessage message = (AudioMessage) object;
            transfer.sendAudio(caption, message.getLinkMp3(), name, "Голосовое сообщение");
        } else if (object instanceof AttachedLink) {
            AttachedLink link = (AttachedLink) object;
            transfer.sendTextMessage(link.getUrl(), name);
        } else if (object instanceof WallPost) {
            WallPost post = (WallPost) object;
            String from = getVKName(post.getFromId());
            transfer.sendTextMessage(caption + "\n**Переслано из " + from + "**\n" + post.getText(), name);

            if (!post.getAttachments().isEmpty()) {
                post.getAttachments().forEach(attachment -> {
                    try {
                        this.onAttachmentMessage(attachment.getAttachmentObject(), name, "");
                    } catch (VkApiException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private String getVKName(int senderId) throws VkApiException {
        String from = "";
        String id = String.valueOf(senderId);
        if (id.contains("-")) {
            Community group = vk.groups.getById()
                    .setGroupId(id.replaceAll("-", "")).execute().getResponseObject()
                    .get(0);
            from = group.getName();
        } else {
            User user = vk.users.get().setUserIds(id).execute().getResponseObject()
                    .get(0);
            from = user.getFirstName() + " " + user.getLastName();
        }
        return from + " (VK)";
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }
}
