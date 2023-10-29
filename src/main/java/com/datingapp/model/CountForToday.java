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
    private Integer count;
    private LocalDateTime time;

    public void increment() {
        ++count;
    }
}
