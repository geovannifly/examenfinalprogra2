package com.miumg.chatbottelegram.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miumg.chatbottelegram.model.ErrorLog;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}