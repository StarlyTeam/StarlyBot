package kr.starly.discordbot.entity;


/**
 * This class is requiring data of ticket
 * @param id by create user
 * @param title title from a modal
 * @param description this is a part of request of message from a modal
 * @param date of create
 */
public record TicketInfo(String id, String title, String description, String date) {
}
