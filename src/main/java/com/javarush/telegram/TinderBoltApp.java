package com.javarush.telegram;

import ch.qos.logback.core.joran.conditional.IfAction;
import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = ""; //TODO: добавь имя бота в кавычках

    public static final String TELEGRAM_BOT_TOKEN = ""; //TODO: добавь токен бота в кавычках

    public static final String OPEN_AI_TOKEN = ""; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo she;
    private int questionCount;


    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();
        start(message);
        gpt(message);
        date(message);
        messageYourName(message);
        profileTinder(message);
        opener(message);

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }

    private void start(String message){
        if(message.equals("/start")){
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            sendTextMessage("Чат бот");
            sendTextMessage(loadMessage("main"));

            showMainMenu("— главное меню бота","/start",
                    "— генерация Tinder-профля \uD83D\uDE0E","/profile",
                    "— сообщение для знакомства \uD83E\uDD70","/opener",
                    "— переписка от вашего имени \uD83D\uDE08","/message",
                    "— переписка со звездами \uD83D\uDD25","/date",
                    "— задать вопрос чату GPT \uD83E\uDDE0","/gpt");
            return;
        }
    }
    private void profileTinder(String message){
        if(message.equals("/profile")){
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько вам лет?");
            return;
        }
        if (currentMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount) {
                case 1 -> {
                    me.age = message;
                    sendTextMessage("Кем вы работаете?");
                    questionCount = 2;
                    return;
                }
                case 2 -> {
                    me.occupation = message;
                    sendTextMessage("Есть у вас хобби?");
                    questionCount = 3;
                    return;
                }
                case 3 -> {
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам НЕ нравится в людях?");
                    return;
                }
                case 4 -> {
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                }
                case 5 -> {
                    me.goals = message;
                    String aboutMyself = me.toString();
                    Message msg = sendTextMessage("Генерация профиля...");
                    String answer = chatGPT.sendMessage(loadPrompt("profile"), aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
                }
            }

            return;
        }
    }
    private void opener(String message){
        if(message.equals("/opener")){
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Имя:");
            return;
        }
        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount) {
                case 1 -> {
                    she.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько лет?");
                    return;
                }
                case 2 -> {
                    she.age = message;
                    questionCount = 3;
                    sendTextMessage("Есть ли у нее хобби и какие?");
                    return;
                }
                case 3 -> {
                    she.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем работает?");
                    return;
                }
                case 4 -> {
                    she.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства:");
                    return;
                }
                case 5 -> {
                    she.goals = message;
                    String aboutFried = she.toString();
                    Message msg = sendTextMessage("Генерация ответа...");
                    String answer = chatGPT.sendMessage(loadPrompt("opener"), aboutFried);
                    updateTextMessage(msg, answer);
                    return;
                }
            }
            return;
        }
    }
    private void messageYourName(String message){
        if(message.equals("/message")){
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите вашу переписку",
                    "Следующее сообщение","message_next",
                    "Пригласить на свидание","message_date");
            return;
        }
        if(currentMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("message_")){
                String prompt = loadPrompt(query);

                String userChatHistory = String.join("\n\n",list);

                Message msg = sendTextMessage("Подождите - ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);

                updateTextMessage(msg,answer);
                return;
            }
            list.add(message);
            return;
        }
    }
    private void date(String message){
        if(message.equals("/date")){
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Арианна Гранде","date_w_grande",
                    "Марго Робби","date_w_robbie",
                    "Зендея","date_w_zendaya",
                    "Райан Гослинг","date_m_gosling",
                    "Том Харди","date_m_hardy");

            return;
        }
        if(currentMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("date_w")){
                sendPhotoMessage(query);
                sendTextMessage("Твоя задача пригласить девушку на свидание за 5 сообщений");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }else if(query.startsWith("date_m")){
                sendPhotoMessage(query);
                sendTextMessage("Твоя задача пригласить парня на свидание за 5 сообщений");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }
            String answer = chatGPT.addMessage(message);
            sendTextMessage(answer);
            return;
        }
    }
    private void gpt(String message){
        if(message.equals("/gpt")){
            currentMode = DialogMode.GPT;
            String text = loadMessage("gpt");
            sendPhotoMessage("gpt");
            sendTextMessage(text);

            return;
        }

        if(currentMode == DialogMode.GPT && !isMessageCommand()){
            String prompt = loadPrompt("gpt");
            Message msg = sendTextMessage("Подождите - ChatGPT думает...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
        }
    }
}
