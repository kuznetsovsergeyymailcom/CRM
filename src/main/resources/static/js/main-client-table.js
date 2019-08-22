var invisibleStatuses, emailTmpl, verifiedUsers, newUsers, mentors, usersWithoutMentors;

//Search clients in main
function clientsSearch() {
    $("#search-clients").keyup(function () {
        let jo = $(".portlet");
        let jo2 = jo.find($(".search_text"));
        let data = this.value.toLowerCase().split(" ");
        this.value.localeCompare("") === 0 ? jo.show() : jo.hide();

        for (let i = 0; i < jo2.length; i++) {
            let count = 0;
            for (let z = 0; z < data.length; z++) {
                if (jo2[i].innerText.toLowerCase().includes(data[z])) {
                    count++;
                }
            }
            if (count === data.length) {
                jo[i].style.display = 'block';
            }
        }
    });
}

//Заготовка главной функции для отрисовки Доски на клиенте
$(document).ready(function renderMainClientTable () {

});

//Получаем список всех скрытых статусов
function getInvisibleStatuses() {
    let url = "/rest/status/all/invisible";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            invisibleStatuses = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Получаем список всех шаблонов рассылки
function getMessageTemplates() {
    let url = "/rest/message-template";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            emailTmpl = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Получаем список всех верифицированных пользователей
function getVerifiedUsers() {
    let url = "/rest/user/isverified";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            verifiedUsers = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Получаем список всех неверифицированных (новых) пользователей
function getUnverifiedUsers() {
    let url = "/rest/user/unverified";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            newUsers = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Получаем список всех менторов
function getAllMentor() {
    let url = "/rest/user/mentors";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            mentors = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Получаем список всех пользователей без менторов
function getAllWithoutMentors() {
    let url = "/rest/user/usersWithoutMentors";
    $.ajax({
        type: 'GET',
        url: url,
        async: false,
        success: function (response) {
            usersWithoutMentors = response;
        },
        error: function (error) {
            console.log(error);
        }
    });
}

//Заполняем таблицу всех скрытых статусов
function drawHiddenStatusesTable() {
    getInvisibleStatuses();
    let element = $('#tr-hidden-statuses');
    let trHTML = '';
    //Очистка содержимого таблицы после ключевого элемента
    element.nextAll().remove();
    if (invisibleStatuses.length !=0) {
        for (let i = 0; i < invisibleStatuses.length; i++) {
            if (invisibleStatuses[i].name != 'deleted') {
                trHTML += "<tr><td width='70%'>" + invisibleStatuses[i].name + "</td>" +
                    "<td>" +
                        "<button type='button' class='show-status-btn btn' " +
                            "value='" + invisibleStatuses[i].id + "'>Показать</button>" +
                    "</td></tr>";
            }
        }
        element.after(trHTML);
    } else {
        element.after("<p>Пусто</p>");
    }
}

//Заполняем список всех шаблонов рассылки
function drawMessageTemplateList(clientId) {
    getMessageTemplates();
    let element = $("#messageTemplateList" + clientId);
    let liHTML = '';
    if (emailTmpl.length != 0) {
        for (let i = 0; i < emailTmpl.length; i++) {
            liHTML += "<li id='eTemplate" + emailTmpl[i].id + "'>";
            if (!emailTmpl[i].templateText.includes("%bodyText%")) {
                liHTML += "<a class='glyphicon glyphicon-ok portlet-send-btn' " +
                    "id='eTemplateBtn" + emailTmpl[i].id + "' data-toggle='modal' " +
                    "data-target='#sendTemplateModal' " +
                    "data-template-id='" + emailTmpl[i].id + "'>" + " " + emailTmpl[i].name + "</a>";
            } else {
                liHTML += "<a class='glyphicon glyphicon-plus portlet-custom-btn' " +
                    "id='eTemplateBtn" + emailTmpl[i].id + "' data-toggle='modal' " +
                    "data-target='#customMessageTemplate' " +
                    "data-template-id='" + emailTmpl[i].id + "'>" + " " + emailTmpl[i].name + "</a>";
            }
            liHTML += "</li>";
        }
        element.empty();
        element.append(liHTML);
    } else {
        element.empty();
        element.append("<li>Список шаблонов рассылки - пуст!</li>");
    }
}

//Заполняем таблицу всех верифицированных пользователей
function drawVerifiedUsersTable() {
    getVerifiedUsers();
    let element = $('#tr-verified-users');
    let trHTML = '';
    //Очистка содержимого таблицы после ключевого элемента
    element.nextAll().remove();
    if (verifiedUsers.length !=0) {
        for (let i = 0; i < verifiedUsers.length; i++) {
            if (verifiedUsers[i].enabled) {
                trHTML += "<tr><td>" + verifiedUsers[i].firstName +
                    " " + verifiedUsers[i].lastName + "</td>";
            } else {
                trHTML += "<tr><td class='unEnabledUser'>" + verifiedUsers[i].firstName +
                    " " + verifiedUsers[i].lastName + "</td>";
            }
            trHTML += "<td class='editUserButtons'>" +
                "<a href='/admin/user/" + verifiedUsers[i].id + "'>" +
                "<button type='button' class='glyphicon glyphicon glyphicon-edit'></button>" +
                "</a>";
            if (userLoggedIn.authorities.some(arrayEl => arrayEl.authority = 'OWNER')) {
                if (verifiedUsers[i].id != userLoggedIn.id) {
                    trHTML += "<button type='button' data-toggle='modal' class='glyphicon glyphicon-remove'" +
                        "data-target='#reAvailableUserModal" + verifiedUsers[i].id + "'></button>";
                    trHTML += "<button type='button' data-toggle='modal' class='glyphicon glyphicon-trash'" +
                        "data-id='" + verifiedUsers[i].id + "' onclick='fillUsersTableForDelete(this)'></button>";
                }
            }
            trHTML += "</td></tr>";
        }
        element.after(trHTML);
    } else {
        element.after("<p>Пусто</p>");
    }
}

//Заполняем таблицу новых (неверифицированных) пользователей
function drawNewUsersTable() {
    getUnverifiedUsers();
    let element = $('#tr-new-user');
    let trHTML = '';
    //Очистка содержимого таблицы после ключевого элемента
    element.nextAll().remove();
    if (newUsers.length !=0) {
        for (let i = 0; i < newUsers.length; i++) {
            trHTML += "<tr><td>" + newUsers[i].firstName + " " + newUsers[i].lastName + "</td>" +
                "<td class='editUserButtons'>" +
                "<a href='/admin/user/" +  newUsers[i].id + "'>" +
                "<button type='button' class='glyphicon glyphicon-ok'></button>" +
                "</a>" +
                "<button type='button' data-toggle='modal' class='glyphicon glyphicon-remove'" +
                "data-target='#deleteNewUserModal" + newUsers[i].id + "'</button>" +
                "</td></tr>";
        }
        element.after(trHTML);
    } else {
        element.after("<p>Пусто</p>");
    }
}

//Отрисовываем меню карточки клиента
function drawClientCardMenu(clientId) {
    getInvisibleStatuses();
    getAllMentor();
    getAllWithoutMentors();
    let element = $(".open-description-btn[data-id=" + clientId + "]");
    let liHTML = '';
    let divider = "<li class='divider'></li>";
    element.prevAll().remove();
    if (userLoggedIn.authorities.some(arrayEl => (arrayEl.authority === 'OWNER') || (arrayEl.authority === 'ADMIN') || (arrayEl.authority === 'HR'))) {
        //Список работников
        liHTML += "<li class='dropdown-header'>Назначить работника:</li>";
        if (usersWithoutMentors.length !=0) {
            for (let i = 0; i < usersWithoutMentors.length; i++) {
                liHTML += "<li><a onclick='assignUser(" + clientId + ", " + usersWithoutMentors[i].id + ", " + userLoggedIn.id + ")'>" +
                    usersWithoutMentors[i].fullName + "</a></li>";
            }
        } else {
            liHTML += "<li>Список работников - пуст!</li>";
        }
        liHTML += divider;

        //Список менторов
        liHTML += "<li class='dropdown-header'>Назначить ментора:</li>";
        if (mentors.length !=0) {
            for (let i = 0; i < mentors.length; i++) {
                liHTML += "<li><a onclick='assignMentor(" + clientId + ", " + mentors[i].id + ", " + userLoggedIn.id + ")'>" +
                    mentors[i].fullName + "</a></li>";
            }
        } else {
            liHTML += "<li>Список менторов - пуст!</li>";
        }
        liHTML += divider;

        //Список скрытых статусов
        liHTML += "<li class='dropdown-header'>Скрыть в статус:</li>";
        if (invisibleStatuses.length !=0) {
            for (let i = 0; i < invisibleStatuses.length; i++) {
                if (invisibleStatuses[i].name != 'deleted') {
                    liHTML += "<li><a class='invisible-client'" +
                        "onclick='invisibleClient(" +clientId + ", " + invisibleStatuses[i].id + ")'>" +
                        invisibleStatuses[i].name + "</a></li>";
                }
            }
        } else {
            liHTML += "<li>Нет скрытых статусов!</li>";
        }
        liHTML += divider;

        element.before(liHTML);
    }
}