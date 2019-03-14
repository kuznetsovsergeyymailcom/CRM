package com.ewp.crm.controllers.rest;

import com.ewp.crm.configs.inteface.VKConfig;
import com.ewp.crm.exceptions.member.NotFoundMemberList;
import com.ewp.crm.models.User;
import com.ewp.crm.models.VkMember;
import com.ewp.crm.models.VkTrackedClub;
import com.ewp.crm.service.interfaces.MessageTemplateService;
import com.ewp.crm.service.interfaces.VKService;
import com.ewp.crm.service.interfaces.VkMemberService;
import com.ewp.crm.service.interfaces.VkTrackedClubService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.queries.ads.AdsGetBudgetQuery;
import com.vk.api.sdk.queries.ads.AdsGetStatisticsIdsType;
import com.vk.api.sdk.queries.ads.AdsGetStatisticsPeriod;
import com.vk.api.sdk.queries.ads.AdsGetStatisticsQuery;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN', 'USER')")
@RequestMapping("/rest/vkontakte")
public class VkRestController {

	private static Logger logger = LoggerFactory.getLogger(VkRestController.class);

	private final VKService vkService;
	private final VkTrackedClubService vkTrackedClubService;
	private final VkMemberService vkMemberService;
	private final MessageTemplateService messageTemplateService;
	private final VKConfig vkConfig;
	private String clientId;
	private String clientSecret;
	private String accountId;
	private String serverPort;


	@Autowired
	public VkRestController(VKService vkService,
							VkTrackedClubService vkTrackedClubService,
							VkMemberService vkMemberService,
							MessageTemplateService messageTemplateService,
							VKConfig vkConfig,
	                        Environment environment) {
		this.vkService = vkService;
		this.vkTrackedClubService = vkTrackedClubService;
		this.vkMemberService = vkMemberService;
		this.messageTemplateService = messageTemplateService;
		this.vkConfig = vkConfig;
		this.clientId = environment.getRequiredProperty("vk.robot.app.clientId");
		this.clientSecret = environment.getRequiredProperty("vk.robot.app.clientSecret");
		this.accountId = environment.getRequiredProperty("vk.accountId");
		this.serverPort = environment.getRequiredProperty("server.port");
	}

    @PostMapping
    public ResponseEntity<String> sendToVkontakte(@RequestParam("clientId") Long clientId,
                                                  @RequestParam("templateId") Long templateId,
                                                  @RequestParam(value = "body",required = false) String body,
												  @AuthenticationPrincipal User userFromSession) {
        String templateText = messageTemplateService.get(templateId).getOtherText();
        vkService.sendMessageToClient(clientId, templateText, body, userFromSession);
        return ResponseEntity.status(HttpStatus.OK).body("Message send successfully");
    }

    @GetMapping(value = "/trackedclub", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VkTrackedClub>> getAllTrackedClub() {
        List<VkTrackedClub> vkTrackedClubs = vkTrackedClubService.getAll();
        return ResponseEntity.ok(vkTrackedClubs);
    }

	@PostMapping(value = "/trackedclub/update")
	public ResponseEntity updateVkTrackedClub(@RequestParam Long id,
											  @RequestParam String groupName,
											  @RequestParam String token,
											  @AuthenticationPrincipal User userFromSession) {
		VkTrackedClub vkTrackedClub = vkTrackedClubService.get(id);
		vkTrackedClub.setGroupName(groupName);
		vkTrackedClub.setToken(token);
		vkTrackedClubService.update(vkTrackedClub);
		logger.info("{} has updated VkTrackedClub: club id {}", userFromSession.getFullName(), vkTrackedClub.getGroupId());
		return ResponseEntity.ok(HttpStatus.OK);
	}

	@PostMapping(value = "/trackedclub/delete")
	public ResponseEntity deleteVkTrackedClub(@RequestParam Long deleteId,
											  @AuthenticationPrincipal User userFromSession) {
		VkTrackedClub currentClub = vkTrackedClubService.get(deleteId);
		vkTrackedClubService.delete(deleteId);
		logger.info("{} has deleted VkTrackedClub: club name {}, id {}", userFromSession.getFullName(),
																		currentClub.getGroupName(), currentClub.getGroupId());
		return ResponseEntity.ok(HttpStatus.OK);
	}

	@PostMapping(value = "/trackedclub/add")
	public ResponseEntity addVkTrackedClub(@RequestParam String groupId,
										   @RequestParam String groupName,
										   @RequestParam String token,
										   @RequestParam String clientId,
										   @AuthenticationPrincipal User userFromSession) {
		VkTrackedClub newVkClub = new VkTrackedClub(Long.parseLong(groupId),
													token, groupName,
													Long.parseLong(clientId));
		int countNewMembers = 0;
		List<VkMember> memberList = vkMemberService.getAllMembersByGroupId(newVkClub.getGroupId());
		List<VkMember> newMemberList = vkService.getAllVKMembers(newVkClub.getGroupId(), 0L)
				.orElseThrow(NotFoundMemberList::new);
		if (memberList.isEmpty()) {
			vkMemberService.addAllMembers(newMemberList);

			logger.info("{} has added vkTrackedClub: group id {}, group name {}", userFromSession.getFullName(),
					newVkClub.getGroupId(), newVkClub.getGroupName());
		} else {
			for (VkMember newVkMember : newMemberList) {
				if(!memberList.contains(newVkMember)){
					vkMemberService.add(newVkMember);
					countNewMembers++;
				}
			}
			logger.info("{} has reloaded vkTrackedClub: group id {}, group name {} with {} new VKMembers", userFromSession.getFullName(),
					newVkClub.getGroupId(), newVkClub.getGroupName(), countNewMembers);
		}
		vkTrackedClubService.add(newVkClub);
		return ResponseEntity.ok(HttpStatus.OK);
	}

	@GetMapping(value = "/connectParam")
	public Map<String, String> vkGetAccessToken() {

		Map<String, String> param = new HashMap<>();
		param.put("groupID", vkConfig.getClubId());
		param.put("accessToken", vkConfig.getCommunityToken());
		param.put("version", vkConfig.getVersion());
		param.put("url", vkConfig.getVkAPIUrl());

		return param;
	}

	@GetMapping(value = "/getProfilePhotoById", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getProfilePhotoLinkById(@RequestParam String vkref){
		String profilePhotoLink = vkService.getVkPhotoLinkByClientProfileId(vkref);
		return ResponseEntity.ok(profilePhotoLink);
	}



	@GetMapping("/ads")
	public String getToken(@RequestParam String code) throws ClientException, ApiException, JSONException {
		TransportClient transportClient = HttpTransportClient.getInstance();
		VkApiClient vk = new VkApiClient(transportClient);
		UserAuthResponse authResponse = vk.oauth()
				.userAuthorizationCodeFlow(Integer.valueOf(clientId), clientSecret, "http://localhost:" + serverPort + "/rest/vkontakte/ads", code)
				.execute();
		String accessToken = authResponse.getAccessToken();
        Long clicks = 0L;
        String spent = "";
        String budget = "";

		UserActor actor = new UserActor(authResponse.getUserId(), accessToken);
		AdsGetBudgetQuery advertisement = vk.ads().getBudget(actor, Integer.parseInt(accountId));
		JSONObject balance = new JSONObject(advertisement.executeAsString());
		budget = balance.getString("response");

		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat simpleDate = new SimpleDateFormat("dd.MM.yyyy");
		String dateToFrom = simpleDateFormat.format(date);

        String uriBudget = "https://api.vk.com/method/" + "ads.getBudget" +
                "?account_id=" + "1605137078" +
                "&version=" + "5.78" +
                "&access_token=" + "524fb23e95f0049733785f33b6f33b09d7cb397d40c03d2a08fadc84dcbab2834452908c75719a7a56a2f";
        HttpGet httpGetBudget = new HttpGet(uriBudget);
        HttpClient httpClientBudget = getHttpClient();
        HttpResponse responseBudget = null;
        JSONObject jsonBudget = null;
        try {
            responseBudget = httpClientBudget.execute(httpGetBudget);
            String resultStat = EntityUtils.toString(responseBudget.getEntity());
            jsonBudget = new JSONObject(resultStat);
            System.out.println("jsonBudget extracted = " + jsonBudget.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }



        String uriStat = "https://api.vk.com/method/" + "ads.getStatistics" +
                "?account_id=" + "1605137078" +
                "&ids_type=" + "office" +
                "&ids=" + "1605137078" +
                "&period=" + "day" +
                "&date_from=" + "2019-03-15" +
                "&date_to=" + "2019-03-15" +
                "&version=" + "5.78" +
                "&access_token=" + "524fb23e95f0049733785f33b6f33b09d7cb397d40c03d2a08fadc84dcbab2834452908c75719a7a56a2f";

        HttpGet httpGetStat = new HttpGet(uriStat);
        HttpClient httpClientStat = getHttpClient();
        HttpResponse responseStat = null;
        JSONObject jsonStat = null;
        try {
            responseStat = httpClientStat.execute(httpGetStat);
            String resultStat = EntityUtils.toString(responseStat.getEntity());
            jsonStat = new JSONObject(resultStat);
            System.out.println("jsonStat extracted = " + jsonStat.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

		AdsGetStatisticsQuery advertisement3 = vk.ads().getStatistics(actor,
				Integer.parseInt(accountId),
				AdsGetStatisticsIdsType.OFFICE,
				accountId,
				AdsGetStatisticsPeriod.DAY,
				dateToFrom,
				dateToFrom);
		String jString = new JSONObject(advertisement3.executeAsString()).toString();



        JSONArray responce = new JSONObject(jString).getJSONArray("response");
        StringBuilder statStr = new StringBuilder("Статистика по вк-рекламному кабинету:").append(System.lineSeparator());
        statStr.append("Дата: ").append(simpleDate.format(date)).append(System.lineSeparator());
        for (int i = 0; i < responce.length() ; i++) {
            JSONObject item = responce.getJSONObject(i);
            if(item.has("stats")) {
                JSONArray stats = item.getJSONArray("stats");
                for (int j = 0; j < stats.length() ; j++) {
                    JSONObject aim = stats.getJSONObject(j);
                    if(aim.has("clicks")) {
                        clicks = aim.getLong("clicks");
                        statStr.append("Количество кликов: ").append(clicks).append(System.lineSeparator());
                    }
                    if (aim.has("spent")) {
                        spent = aim.getString("spent");
                        statStr.append("Денег потрачено: ").append(spent).append(System.lineSeparator());
                    }
                }
            }
        }
        statStr.append("Баланс: ").append(budget);
        System.out.println(statStr);
		return statStr.toString();
	}

    public HttpClient getHttpClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
    }

}
