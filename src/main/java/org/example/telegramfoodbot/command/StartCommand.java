package org.example.telegramfoodbot.command;

import lombok.RequiredArgsConstructor;
import org.example.telegramfoodbot.bot.BotSender;
import org.example.telegramfoodbot.service.ProfileService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class StartCommand {

    private final BotSender sender;
    private final ProfileService profileService;
    private final SetupCommand setupCommand;

    public void handle(Message msg) {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();
        String name = msg.getFrom().getFirstName();

        if (profileService.findByTelegramId(userId).isEmpty()) {
            sender.send(chatId, "Привіт, " + name + "! Я — бот для підрахунку калорій.\n\nДавай налаштуємо профіль:");
            setupCommand.start(msg);
        } else {
            sender.sendMenu(chatId, "З поверненням, %s! 👋\n\nКидай фото їжі або обирай дію:".formatted(name));
        }
    }
}
