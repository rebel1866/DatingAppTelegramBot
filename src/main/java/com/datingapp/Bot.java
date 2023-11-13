package com.datingapp;

import com.datingapp.model.CountForToday;
import com.datingapp.model.Phrase;
import com.datingapp.model.User;
import feign.Feign;
import feign.FeignException;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class Bot extends TelegramLongPollingBot {
    @Value("${bot.name}")
    @Getter
    private String botUsername;

    @Value("${bot.token}")
    @Getter
    private String botToken;

    @Value("${bot.city}")
    @Getter
    private String city;
    @Value("${bot.host}")
    private String host;
    @Value("${bot.port}")
    private String port;
    private UserClient userClient;

    private CountForToday count;

    private boolean isAwaitingMessage;
    private boolean isAwaitingAddMany;
    private boolean isAwaitingPhraseId;
    @Value("${bot.vk.token}")
    private String token;
    @Value("${bot.allowedChatId}")
    private Long allowedChatId;
    private User currentUser;

    @PostConstruct
    public void init() {
        userClient = Feign.builder()
                .client(new OkHttpClient())
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Slf4jLogger(UserClient.class))
                .logLevel(Logger.Level.FULL)
                .target(UserClient.class, "http://" + host + ":" + port);
        count = new CountForToday(0,0, LocalDateTime.now());
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.getMessage().getChatId().equals(allowedChatId)) {
                System.out.println("Somebody else is trying to connect. Chat id: " + update.getMessage().getChatId());
                System.out.println("Allowed id: " + allowedChatId);
                return;
            }
            String input = update.getMessage().getText();
            if (token == null && !input.startsWith("Token")) {
                sendMessage(update.getMessage().getChatId(), "Please, update token");
                return;
            }
            if (input.startsWith("Token")) {
                token = input.split("=")[1];
                sendMessage(update.getMessage().getChatId(), "Updated");
                return;
            }
            if (isAwaitingAddMany) {
                if (input.equals("/cancel")) {
                    sendMessage(update.getMessage().getChatId(), "Canceled");
                } else {
                    try {
                        if (LocalDateTime.now().getDayOfMonth() != count.getTime().getDayOfMonth()) {
                            count = new CountForToday(0,0, LocalDateTime.now());
                        }
                        List<User> users = userClient.getUsers(Integer.parseInt(input), city);
                        users.forEach(user -> {
                            userClient.setViewed(user.getId());
                            userClient.addToFriends(user.getId(), token);
                            count.incrementSeen();
                            count.incrementAdded();
                        });
                    } catch (FeignException e) {
                        sendMessage(update.getMessage().getChatId(), getErrorMessage(e.getMessage()));
                        isAwaitingAddMany = false;
                        return;
                    }
                    sendMessage(update.getMessage().getChatId(), "Success");
                }
                isAwaitingAddMany = false;
                return;
            }
            if (isAwaitingMessage) {
                if (input.equals("/cancel")) {
                    sendMessage(update.getMessage().getChatId(), "Canceled");
                } else {
                    Map<String, String> response;
                    try {
                        response = userClient.send(currentUser.getId(), input, token);
                    } catch (FeignException e) {
                        sendMessage(update.getMessage().getChatId(), getErrorMessage(e.getMessage()));
                        isAwaitingMessage = false;
                        return;
                    }
                    sendMessage(update.getMessage().getChatId(), response.get("response"));
                }
                isAwaitingMessage = false;
                return;
            }
            if (isAwaitingPhraseId) {
                if (input.equals("/cancel")) {
                    sendMessage(update.getMessage().getChatId(), "Canceled");
                } else {
                    Map<String, String> response;
                    try {
                        response = userClient.sendPhraseById(currentUser.getId(), Long.parseLong(input), token);
                    } catch (FeignException e) {
                        sendMessage(update.getMessage().getChatId(), getErrorMessage(e.getMessage()));
                        isAwaitingPhraseId = false;
                        return;
                    }
                    sendMessage(update.getMessage().getChatId(), response.get("response"));
                }
                isAwaitingPhraseId = false;
                return;
            }
            String inputCommand = update.getMessage().getText();
            switch (inputCommand) {
                case "/next":
                    handleNext(update);
                    break;
                case "/add":
                    handleAdd(update);
                    break;
                case "/favorite":
                    handleFavorite(update);
                    break;
                case "/randomphrase":
                    handleRandom(update);
                    break;
                case "/phrase":
                    handlePhrase(update);
                    break;
                case "/message":
                    handleMessage(update);
                    break;
                case "/addmany":
                    handleAddMany(update);
                    break;
                case "/count":
                    sendMessage(update.getMessage().getChatId(), "Users added today: " + count.getCountAdded()
                    + "\n Amount seen today: " + count.getCountSeen());
                    break;
                case "/info":
                    sendMessage(update.getMessage().getChatId(), "To update token send message \"Token={token}\"" +
                            "\nUse https://vkhost.github.io/ to obtain token (choose VkAdmin)");
                    break;
                default:
                    sendMessage(update.getMessage().getChatId(),"Unknown command");
                    break;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handlePhrase(Update update) throws TelegramApiException {
        List<Phrase> phraseList = userClient.getAllPhrases();
        isAwaitingPhraseId = true;
        StringBuilder text = new StringBuilder();
        text.append("Choose phrase ID: ").append("\n");
        phraseList.forEach(phrase -> text.append(phrase.getId()).append(" - ").append(phrase.getPhraseText()).append("\n"));
        sendMessage(update.getMessage().getChatId(), text.toString());
    }

    private void handleRandom(Update update) throws TelegramApiException {
        Map<String, String> response;
        try {
            response = userClient.sendRandomPhrase(currentUser.getId(), token);
        } catch (FeignException e) {
            sendMessage(update.getMessage().getChatId(), getErrorMessage(e.getMessage()));
            return;
        }
        sendMessage(update.getMessage().getChatId(), response.get("response"));
    }

    private String getErrorMessage(String input) {
        if (input.contains("error")) {
            String[] ar = input.split("error");
            if (ar.length > 1) {
                String message = ar[1];
                return message.replaceAll("[\":\\[\\]{}]", "");
            }
        }
        return input;
    }

    private void handleMessage(Update update) throws TelegramApiException {
        sendMessage(update.getMessage().getChatId(), "Enter message:");
        isAwaitingMessage = true;
    }
    private void handleAddMany(Update update) throws TelegramApiException {
        sendMessage(update.getMessage().getChatId(), "Enter amount:");
        isAwaitingAddMany = true;
    }


    private void handleFavorite(Update update) throws TelegramApiException {
        if (currentUser != null) {
            Map<String, String> responseVk = userClient.addFavorite(currentUser.getId());
            Map<String, String> responseApp = userClient.addAppFavorite(currentUser.getId());
            String result = responseApp.get("response").equals("success") &&
                    responseVk.get("response").equals("success") ? "success": "not sucess";
            sendMessage(update.getMessage().getChatId(), result);
        }
    }

    private void handleAdd(Update update) throws TelegramApiException {
        // checkToken();
        if (LocalDateTime.now().getDayOfMonth() != count.getTime().getDayOfMonth()) {
            count = new CountForToday(0,0, LocalDateTime.now());
        } else {
            count.incrementAdded();
        }
        if (currentUser != null & token != null) {
            Map<String, String> response = userClient.addToFriends(currentUser.getId(), token);
            sendMessage(update.getMessage().getChatId(), response.get("response"));
        }
    }

    private void handleNext(Update update) throws TelegramApiException {
        List<User> users = userClient.getUsers(1, city);
        User user = users.get(0);
        currentUser = user;
        if (LocalDateTime.now().getDayOfMonth() != count.getTime().getDayOfMonth()) {
            count = new CountForToday(0,0, LocalDateTime.now());
        } else {
            count.incrementSeen();
        }
        Long chatId = update.getMessage().getChatId();
        userClient.setViewed(user.getId());
        sendMessageWithUserInfo(chatId, user);
        sendMediaGroup(chatId, user.getPhotos(), user.getFirstName() + " " + user.getLastName());
    }

    private void sendMessageWithUserInfo(Long chatId, User user) throws TelegramApiException {
        sendMessage(chatId, createMessage(user));
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        execute(sendMessage);
    }

    private String createMessage(User user) {
        StringBuilder result = new StringBuilder();
        appendKeyValue(result, "Name", user.getFirstName());
        appendKeyValue(result, "Last name", user.getLastName());
        appendKeyValue(result, "Age", user.getAge());
        appendKeyValue(result, "ID", user.getId());
        appendKeyValue(result, "Link", "https://vk.com/id" + user.getId());
        appendKeyValue(result, "Birth date", user.getBdate());
        appendKeyValue(result, "City", user.getCityName());
        appendKeyValue(result, "Is friend", user.getIsFriend());
        appendKeyValue(result, "Can write message", user.getCanWritePrivateMessage());
        appendKeyValue(result, "Can add to friends", user.getCanSendFriendRequest());
        appendKeyValue(result, "Is added to favorite", user.getIsVkFavorite());
        appendKeyValue(result, "Interests", user.getInterests());
        appendKeyValue(result, "Books", user.getBooks());
        appendKeyValue(result, "Quotes", user.getQuotes());
        appendKeyValue(result, "About", user.getAbout());
        appendKeyValue(result, "Movies", user.getMovies());
        appendKeyValue(result, "Activities", user.getActivities());
        appendKeyValue(result, "Music", user.getMusic());
        appendKeyValue(result, "Mobile phone", user.getMobilePhone());
        appendKeyValue(result, "University", user.getUniversityName());
        appendKeyValue(result, "Faculty", user.getFacultyName());
        appendKeyValue(result, "People main", user.getPeopleMain());
        appendKeyValue(result, "Smoking", user.getSmoking());
        appendKeyValue(result, "Religion", user.getReligion());
        appendKeyValue(result, "Alcohol", user.getAlcohol());
        appendKeyValue(result, "Inspired by", user.getInspiredBy());
        appendKeyValue(result, "Life main", user.getLifeMain());
        appendKeyValue(result, "Relation", user.getRelation());
        appendKeyValue(result, "Friends amount", user.getFriendsAmount());
        appendKeyValue(result, "Beauty", user.getAttractiveness());
        return result.toString();
    }

    private void appendKeyValue(StringBuilder result, String key, String value) {
        if (!StringUtils.isBlank(value)) {
            appendKeyValuePair(key, value, result);
        }
    }

    private void appendKeyValue(StringBuilder result, String key, Object value) {
        if (value != null) {
            if (value instanceof Boolean val) {
                value = val ? "yes" : "no";
            }
            appendKeyValuePair(key, value, result);
        }
    }

    private void appendKeyValuePair(String key, Object value, StringBuilder result) {
        result.append(key);
        result.append(": ");
        result.append(value);
        result.append("\n");
    }


    private void sendMediaGroup(Long chatId, List<String> photoUrls, String caption) throws TelegramApiException {
        List<InputMedia> medias = photoUrls.stream()
                .map(photoUrl -> {
                    String mediaName = UUID.randomUUID().toString();
                    InputStream input;
                    try {
                        input = new URL(photoUrl).openStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return (InputMedia) InputMediaPhoto.builder()
                            .media("attach://" + mediaName)
                            .mediaName(mediaName)
                            .isNewMedia(true).caption(caption)
                            .newMediaStream(input)
                            .parseMode(ParseMode.HTML)
                            .build();
                }).collect(Collectors.toList());

        SendMediaGroup sendMediaGroup = SendMediaGroup.builder()
                .chatId(chatId)
                .medias(medias)
                .build();
        execute(sendMediaGroup);
    }
}