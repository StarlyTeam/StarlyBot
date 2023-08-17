package kr.starly.discordbot.command.slash;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface BotSlashCommand {

    String command();
    String description();
    OptionType[] optionType() default {};
    String[] names() default "";
    String[] optionDescription() default {};
}
