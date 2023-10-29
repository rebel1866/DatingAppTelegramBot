package com.datingapp;

import com.datingapp.model.Phrase;
import com.datingapp.model.User;
import feign.Param;
import feign.RequestLine;

import java.util.List;
import java.util.Map;

public interface UserClient {
    @RequestLine("GET /users?amount={amount}&city={city}")
    List<User> getUsers(@Param("amount") Integer amount, @Param("city") String city);

    @RequestLine("GET /users/setViewed/{id}")
    Map<String, String> setViewed(@Param("id") Long id);

    @RequestLine("GET /users/vkFavorites/{id}")
    Map<String, String> addFavorite(@Param("id") Long id);

    @RequestLine("GET /messages/phrases")
    List<Phrase> getAllPhrases();

    @RequestLine("POST /messages/send/{id}/phrases/{phraseId}?token={token}")
    Map<String, String> sendPhraseById(@Param("id") Long id, @Param("phraseId") Long phraseId, @Param("token") String token);

    @RequestLine("POST /messages/send/{id}/phrases?token={token}")
    Map<String, String> sendRandomPhrase(@Param("id") Long id, @Param("token") String token);

    @RequestLine("POST /messages/send/{id}?message={message}&token={token}")
    Map<String, String> send(@Param("id") Long id, @Param("message") String message, @Param("token") String token);

    @RequestLine("POST /messages/addFriend/{id}?token={token}")
    Map<String, String> addToFriends(@Param("id") Long id, @Param("token") String token);
}
