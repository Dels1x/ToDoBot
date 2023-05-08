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

    public User getUserByTag(Update update) {
        org.telegram.telegrambots.meta.api.objects.User tgUser = update.getMessage().getFrom();
        String userTag = tgUser.getUserName();
        Optional<User> user = userRepository.findByTag(userTag);

        if(user.isPresent()) {
            return user.get();
        } else {
            User newUser = User.builder()
                    .name(tgUser.getFirstName())
                    .taskCount(0)
                    .taskCompleted(0)
                    .createdAt(LocalDate.now())
                    .tag(userTag)
                    .build();
            return userRepository.save(newUser);
        }
    }
}
