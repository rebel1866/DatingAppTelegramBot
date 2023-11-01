package com.datingapp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CountForToday {
    private Integer countAdded;
    private Integer countSeen;
    private LocalDateTime time;

    public void incrementAdded() {
        ++countAdded;
    }
    public void incrementSeen() {
        ++countSeen;
    }
}
