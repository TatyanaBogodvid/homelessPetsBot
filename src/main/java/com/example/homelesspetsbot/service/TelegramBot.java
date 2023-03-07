package com.example.homelesspetsbot.service;

import com.example.homelesspetsbot.config.BotConfig;
import com.example.homelesspetsbot.model.User;
import com.example.homelesspetsbot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    @Autowired
    private UserRepository userRepository;
    final private static String INFO = "Мы находимся по адресу Востряковский пр-д, 10А, Москва, 117403" +
            "\n Приют Бирюлево - это муниципальный приют для бездомных собак и кошек в Южном округе г. Москвы. " +
            "\n В нем живет почти 2500 собак и 150 кошек. " +
            "\n Большие и маленькие, пушистые и гладкие, веселые и задумчивые " +
            "- и на всех одна большая мечта - встретить своего Человека и найти Дом.";

    public TelegramBot(BotConfig config) {
        this.config = config;
        /** Создание кнопки меню
         * (все команды должны быть написаны в нижнем регистре)*/
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Приветствует пользователя"));
        listOfCommands.add(new BotCommand("/info", "Выводит информацию о приюте"));
        listOfCommands.add(new BotCommand("/to_adopt", "Выводит информацию о том, как взять питомца из приюта"));
        listOfCommands.add(new BotCommand("/submit_report", "Выводит информацию о том, как прислать отчет о питомце"));
        listOfCommands.add(new BotCommand("/call_volunteer", "Вызвать волотера"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error settings bot's command list: " + e.getMessage());
        }
    }

    @Override
    /**Метод возвращает имя пользователя*/
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    /**Метод возвращает токен пользователя*/
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    /**Обработка кнопок меню*/
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getFirstName();

            switch (messageText){
                case "/start":

                    registerUser(update.getMessage());
                    startCommandReceived(chatId, userName);
                    break;
                case "/info":
                    sendMessage(chatId, INFO);
                    break;
                default: sendMessage(chatId, "Команда пока не поддерживается");
            }
        }
    }

    /**Регистрация пользователь. Если ранее пользователь не был зарегистрирован*/
    private void registerUser(Message message) {
        if(userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }


    /**Метод для /start*/
    private void startCommandReceived(long chatId, String userName){
        String answer = "Привет! МЫ - ВОЛОНТЕРЫ МОСКОВСКОГО МУНИЦИПАЛЬНОГО ПРИЮТА ДЛЯ БЕЗДОМНЫХ ЖИВОТНЫХ БИРЮЛЕВО";

        log.info("Replied to user " + userName);

        sendMessage(chatId,answer);
    }

    /**Отправка сообщения пользователю*/
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try{
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }


}
