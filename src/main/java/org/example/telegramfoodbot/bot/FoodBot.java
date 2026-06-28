package org.example.telegramfoodbot.bot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.telegramfoodbot.command.*;
import org.example.telegramfoodbot.config.BotConfig;
import org.example.telegramfoodbot.handler.PhotoHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Component
public class FoodBot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final SetupCommand setupCommand;
    private final PhotoHandler photoHandler;
    private final TodayCommand todayCommand;
    private final WeekCommand weekCommand;
    private final WeightCommand weightCommand;
    private final EditCommand editCommand;
    private final StartCommand startCommand;

    public FoodBot(BotConfig config,
                   SetupCommand setupCommand,
                   PhotoHandler photoHandler,
                   TodayCommand todayCommand,
                   WeekCommand weekCommand,
                   WeightCommand weightCommand,
                   EditCommand editCommand,
                   StartCommand startCommand) {
        super(config.token());
        this.config = config;
        this.setupCommand = setupCommand;
        this.photoHandler = photoHandler;
        this.todayCommand = todayCommand;
        this.weekCommand = weekCommand;
        this.weightCommand = weightCommand;
        this.editCommand = editCommand;
        this.startCommand = startCommand;
    }

    @PostConstruct
    public void init() {
        String token = config.token();
        log.info(">>> BOT STARTING with token length: {}", token != null ? token.length() : 0);
        log.info(">>> BOT USERNAME: {}", config.username());
        try {
            execute(new DeleteWebhook());
            log.info(">>> Webhook cleared; long polling is active");
        } catch (TelegramApiException e) {
            log.warn(">>> Could not clear webhook: {}", e.getMessage());
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        log.info(">>> BATCH: {} updates", updates.size());
        super.onUpdatesReceived(updates);
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info(">>> UPDATE RECEIVED: {}", update.getUpdateId());
        try {
            if (update.hasCallbackQuery()) {
                var cq = update.getCallbackQuery();
                String data = cq.getData() != null ? cq.getData() : "";
                if (data.startsWith("save_") || data.startsWith("refine_")) {
                    photoHandler.handleCallback(cq);
                } else {
                    setupCommand.handleCallback(cq);
                }
                return;
            }
            if (!update.hasMessage()) return;

            Message msg = update.getMessage();
            long userId = msg.getFrom().getId();

            if (msg.hasPhoto()) {
                photoHandler.handle(msg);
                return;
            }

            if (!msg.hasText()) return;
            String text = msg.getText().trim();

            if (photoHandler.isAwaitingRefinement(userId)) {
                photoHandler.handleRefinement(msg);
                return;
            }

            if (setupCommand.isInSetup(userId)) {
                setupCommand.handleInput(msg);
                return;
            }

            String command = text.split("\\s+")[0].split("@")[0].toLowerCase();
            switch (command) {
                case "/start"  -> startCommand.handle(msg);
                case "/setup"  -> setupCommand.start(msg);
                case "/today"  -> todayCommand.handle(msg);
                case "/week"   -> weekCommand.handle(msg);
                case "/weight" -> weightCommand.handle(msg);
                case "/edit"   -> editCommand.handle(msg);
                default -> {
                    switch (text.toLowerCase()) {
                        case "📊 сьогодні" -> todayCommand.handle(msg);
                        case "📅 тиждень"  -> weekCommand.handle(msg);
                        case "⚖️ вага"     -> weightCommand.handle(msg);
                        case "⚙️ профіль"  -> setupCommand.start(msg);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unhandled error processing update", e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.username();
    }
}
