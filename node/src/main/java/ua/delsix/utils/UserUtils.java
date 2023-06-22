package ua.delsix.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.delsix.entity.User;
import ua.delsix.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class UserUtils {
    private final UserRepository userRepository;

    public UserUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByUpdate(Update update) {
        org.telegram.telegrambots.meta.api.objects.User tgUser;
        
        if(update.hasMessage()) {
            tgUser = update.getMessage().getFrom();
        } else {
            tgUser = update.getCallbackQuery().getFrom();
        }

        Long userId = tgUser.getId();
        Optional<User> user = userRepository.findByTelegramId(userId);

        if(user.isPresent()) {
            return user.get();
        } else {
            String languageCode = tgUser.getLanguageCode();

            if(!languageCode.equals("en") && !languageCode.equals("ua") && !languageCode.equals("ru")) {
                languageCode = "en";
            }

            User newUser = User.builder()
                    .name(tgUser.getFirstName())
                    .taskCount(0)
                    .taskCompleted(0)
                    .createdAt(LocalDate.now())
                    .tag(tgUser.getUserName())
                    .telegramId(userId)
                    .language(languageCode)
                    .build();
            return userRepository.save(newUser);
        }
    }

    public void setLanguage(Update update, String language) {
        User user = getUserByUpdate(update);
        user.setLanguage(language);
        userRepository.save(user);
    }
}
