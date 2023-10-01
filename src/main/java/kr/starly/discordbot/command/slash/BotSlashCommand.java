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
    String[] optionName() default "";
    OptionType[] optionType() default {};
    String[] optionDescription() default {};
    boolean[] optionRequired() default {};

    SubCommand[] subcommands() default {};

    @interface SubCommand {
        String name();
        String description();
        String[] optionName() default "";
        OptionType[] optionType() default {};
        String[] optionDescription() default {};
        boolean[] optionRequired() default {};
    }
}