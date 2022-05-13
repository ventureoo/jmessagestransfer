package com.github.ventureo.jmessagestransfer;

import java.util.Properties;

/*
Abstract message sender via its own API
*/
public interface Transfer {
    /*
     * Send a simple text message
     *
     * @param text - message body
     *
     * @param author - sender name
     */
    void sendTextMessage(String text, String author);

    /*
     * Send photo
     *
     * For VK <-> Telegram: native photo upload
     *
     * @param caption - text message that comes with the video
     *
     * @param url - photo address (URL/File path)
     */
    void sendPhotoMessage(String caption, String url, String author);

    /*
     * Send sticker
     */
    void sendSticker(String url, String author, boolean isAnimated);

    /*
     * Send video
     * 
     * For VK -> Telegram: sends only a link to the video.
     * For Telegram -> VK: sends video as document.
     *
     * @param caption - text message that comes with the video
     * 
     * @param url - video address (URL/File path)
     * 
     * @param author - sender name
     *
     */
    void sendVideo(String caption, String url, String author);

    /*
     * Send document
     * 
     * For VK -> Telegram: sends a direct link to the file.
     * This allows you to bypass Telegram's restrictions on uploading files.
     * For Telegram -> VK: we upload a document, but no more than
     * 20MB, due to Telegram restrictions.
     *
     * @param caption - text message that comes with the document
     * 
     * @param url - document address (URL/File path)
     * 
     * @param author - sender name
     */
    void sendDocument(String caption, String url, String author);

    /*
     * Sends audio recording
     * 
     * For VK -> Telegram: it doesn't work well.
     * That's why we can often get an exception
     * about an audio record not being found.
     * For Telegram -> VK: works like sending a document.
     *
     * @param caption - text message that comes with the audio
     * 
     * @param url - audio address (URL/File path)
     * 
     * @param author - sender name
     */
    void sendAudio(String caption, String url, String author, String title);

    Properties getConfig();
}
