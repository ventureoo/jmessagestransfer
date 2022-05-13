package com.github.ventureo.jmessagestransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.github.ventureo.jmessagestransfer.exceptions.ConfigCreatedException;
import com.github.ventureo.jmessagestransfer.transfers.TelegramTransfer;
import com.github.ventureo.jmessagestransfer.transfers.VkTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferManager {
    public final static String PATH = System.getProperty("user.dir") + File.separator;
    public final static String CONFIG_PATH = PATH + "config.properties";

    private final Logger logger = LoggerFactory.getLogger(TransferManager.class);

    public void start() throws InterruptedException {
        File configFile = new File(CONFIG_PATH);

        try {
            Properties config = loadConfig(configFile);
            VkTransfer vk = new VkTransfer(config);
            TelegramTransfer tg = new TelegramTransfer(config);

            tg.getHandler().setTransfer(vk);
            vk.getHandler().setTransfer(tg);
            tg.start();
            vk.start();
        } catch (IOException e) {
            logger.error(
                    "An error occurred while working with the configuration file." +
                            "\nPlease make sure that you run the program as a user who has write permissions" +
                            "to the files in the directory.",
                    e);
        } catch (ConfigCreatedException e) {
            logger.info("The configuration file was successfully created, edit it for the bot to work properly.");
        }
    }

    private Properties loadConfig(File configFile) throws IOException, ConfigCreatedException {
        Properties config = new Properties();

        if (!configFile.exists()) {
            InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream("config.properties");
            config.load(systemResourceAsStream);
            configFile.createNewFile();
            config.store(new FileOutputStream(configFile), null);
            systemResourceAsStream.close();
            throw new ConfigCreatedException();
        }

        FileReader reader = new FileReader(configFile);
        config.load(reader);
        reader.close();
        return config;
    }
}
