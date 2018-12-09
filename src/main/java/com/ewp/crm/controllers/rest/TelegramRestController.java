package com.ewp.crm.controllers.rest;

import com.ewp.crm.models.SocialProfile;
import com.ewp.crm.service.impl.TelegramServiceImpl;
import com.ewp.crm.service.interfaces.ClientService;
import com.ewp.crm.service.interfaces.SocialProfileService;
import com.ewp.crm.service.interfaces.TelegramService;
import org.drinkless.tdlib.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/rest/telegram")
@PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN', 'USER')")
public class TelegramRestController {

    private final TelegramService telegramService;
    private final ClientService clientService;
    private final SocialProfileService socialProfileService;
    private static final int MESSAGE_LIMIT = 40;

    private static Logger logger = LoggerFactory.getLogger(TelegramRestController.class);

    @Autowired
    public TelegramRestController(TelegramService telegramService, ClientService clientService,
                                  SocialProfileService socialProfileService) {
        this.telegramService = telegramService;
        this.clientService = clientService;
        this.socialProfileService = socialProfileService;
    }

    @GetMapping("/phone-code")
    public HttpStatus sendAuthPhone(@RequestParam("phone") String phone) {
        telegramService.sendAuthPhone(phone);
        return HttpStatus.OK;
    }

    @GetMapping("/sms-code")
    public HttpStatus sendAuthCodeFromSms(@RequestParam("code") String code) {
        telegramService.sentAuthCode(code);
        return HttpStatus.OK;
    }

    @GetMapping("/messages/chat/open")
    public ResponseEntity<Map<String, Object>> getChatMessages(@RequestParam("clientId") Long clientId) {
        List<SocialProfile> profiles =  clientService.getClientByID(clientId).getSocialProfiles();
        TdApi.Messages messages = new TdApi.Messages();
        TdApi.Chat chat = new TdApi.Chat();
        ResponseEntity result = ResponseEntity.badRequest().build();
        for (SocialProfile profile : profiles) {
            if("telegram".equals(profile.getSocialProfileType().getName())) {
                String chatId = profile.getLink();
                messages = telegramService.getChatMessages(Long.parseLong(chatId), MESSAGE_LIMIT);
                chat = telegramService.getChat(Long.parseLong(chatId));
                Map<String, Object> map = new HashMap<>();
                map.put("messages", messages);
                map.put("chat", chat);
                result = new ResponseEntity<>(map, HttpStatus.OK);
                break;
            }
        }
        return result;
    }

    @GetMapping("/messages/chat/close")
    public HttpStatus closeChat(@RequestParam("clientId") Long clientId) {
        List<SocialProfile> profiles =  clientService.getClientByID(clientId).getSocialProfiles();
        HttpStatus result = HttpStatus.NOT_FOUND;
        for (SocialProfile profile : profiles) {
            if("telegram".equals(profile.getSocialProfileType().getName())) {
                String chatId = profile.getLink();
                telegramService.closeChat(Long.parseLong(chatId));
                result = HttpStatus.OK;
                break;
            }
        }
        return result;
    }

    @GetMapping("/messages/chat/unread")
    public ResponseEntity<Map<String, Object>>  getUnreadChatMessages(@RequestParam("clientId") Long clientId) {
        List<SocialProfile> profiles =  clientService.getClientByID(clientId).getSocialProfiles();
        TdApi.Messages messages = new TdApi.Messages();
        TdApi.Chat chat = new TdApi.Chat();
        ResponseEntity result = ResponseEntity.badRequest().build();
        for (SocialProfile profile : profiles) {
            if("telegram".equals(profile.getSocialProfileType().getName())) {
                String chatId = profile.getLink();
                messages = telegramService.getUnreadMessagesFromChat(Long.parseLong(chatId), MESSAGE_LIMIT);
                chat = telegramService.getChat(Long.parseLong(chatId));
                Map<String, Object> map = new HashMap<>();
                map.put("messages", messages);
                map.put("chat", chat);
                result = new ResponseEntity<>(map, HttpStatus.OK);
                break;
            }
        }
        return result;
    }

    @PostMapping("/message/send")
    public ResponseEntity<TdApi.Message> getChatMessages(@RequestParam("clientId") long clientId, @RequestParam("text") String text) {
        Optional<SocialProfile> profile = socialProfileService.getSocialProfileByClientIdAndTypeName(clientId, "telegram");
        ResponseEntity result = ResponseEntity.notFound().build();
        if (profile.isPresent()) {
            result = new ResponseEntity(telegramService.sendChatMessage(Long.parseLong(profile.get().getLink()), text), HttpStatus.OK);
        }
        return result;
    }

    @GetMapping("/me")
    public ResponseEntity<TdApi.User> getCurrentUser() {
        return new ResponseEntity<>(telegramService.getMe(), HttpStatus.OK);
    }

    @GetMapping("/user")
    public ResponseEntity<TdApi.User> getUserById(@RequestParam("id") long clientId) {
        ResponseEntity result = ResponseEntity.notFound().build();
        Optional<SocialProfile> profile = socialProfileService.getSocialProfileByClientIdAndTypeName(clientId, "telegram");
        if (profile.isPresent()) {
            result = new ResponseEntity(telegramService.getUserById(Integer.parseInt(profile.get().getLink())), HttpStatus.OK);
        }
        return result;
    }

    @GetMapping("/file/photo")
    public ResponseEntity<String> getPhotoByFileId(@RequestParam("id") int fileId) {
        TdApi.File file = telegramService.getFileById(fileId);
        String data = "";
        ResponseEntity<String> result = new ResponseEntity<>(data, HttpStatus.NOT_FOUND);
        try {
            data = telegramService.downloadFile(file);
            result = new ResponseEntity<>(data, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Failed to download file {}", file, e);
        }
        return result;
    }

    @GetMapping("/logout")
    public HttpStatus logoutFromTelegram() {
        telegramService.logout();
        return HttpStatus.OK;
    }
}
