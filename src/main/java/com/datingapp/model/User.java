package com.datingapp.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User {
    private Long id;
    private String bdate;
    private String cityName;
    private String firstName;
    private String lastName;
    private Short age;
    private Boolean isFriend;
    private Boolean canWritePrivateMessage;
    private Boolean canSendFriendRequest;
    private Boolean isVkFavorite;
    private Boolean isApplicationFavorite;
    private String interests;
    private String books;
    private String quotes;
    private String about;
    private String movies;
    private String activities;
    private String music;
    private String mobilePhone;
    private String universityName;
    private String facultyName;
    private String peopleMain;
    private String political;
    private String smoking;
    private String religion;
    private String alcohol;
    private String inspiredBy;
    private String lifeMain;
    private String relation;
    private List<String> career;
    private List<String> photos;
    private int friendsAmount;
    private String attractiveness;
}