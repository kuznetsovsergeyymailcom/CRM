package com.ewp.crm.controllers.rest;

import com.ewp.crm.models.User;
import com.ewp.crm.models.UserRoutes;
import com.ewp.crm.models.dto.UserRoutesDto;
import com.ewp.crm.service.interfaces.RoleService;
import com.ewp.crm.service.interfaces.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/rest/hr")
public class HrRestController {
    private static Logger logger = LoggerFactory.getLogger(HrRestController.class);

    private final UserService userService;
    private final RoleService roleService;

    @Autowired
    public HrRestController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/getuserroutesbytype/{userRoutesType}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ResponseEntity<List<User>> getHrPercentDistribution(
            @PathVariable String userRoutesType, @AuthenticationPrincipal User userFromSession) {
        return ResponseEntity.ok(userService.getByRole(roleService.getRoleByName("HR")));
    }

    @PostMapping(value = "/saveroutes")
    @PreAuthorize("hasAnyAuthority('OWNER', 'ADMIN')")
    public ResponseEntity saveHrRoutes(@RequestBody List<UserRoutesDto> userRoutesDtoListist) {
        userService.updateClientRoutes(userRoutesDtoListist);
        return ResponseEntity.ok(userRoutesDtoListist);
    }
}