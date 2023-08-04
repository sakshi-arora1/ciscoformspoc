function elqGetCookie(name) {
    var cookies = document.cookie.split(';');
    for (var i = 0; i < cookies.length; i++) {
        var position = cookies[i].indexOf('=');
        if (position > 0 && position < cookies[i].length - 1) {
            var x = cookies[i].substr(0, position);
            var y = cookies[i].substr(position + 1);
            x = x.replace(/^\s+|\s+$/g, '');
            if (x == name) {
                return unescape(y);
            }
        } 
    }
    return '';
}
function elqGetCookieSubValue(name, subKey) {
    var cookieValue = elqGetCookie(name);
    if (cookieValue == null || cookieValue == '')
        return '';
    var cookieSubValue = cookieValue.split('&');
    for (var i = 0; i < cookieSubValue.length; i++) {
        var pair = cookieSubValue[i].split('=');
        if (pair.length > 1) {
            if (pair[0] == subKey) {
                return pair[1];
            }
        }
    }
    return '';
}
function elqSetCustomerGUID() {
    var elqCustomerGuid = elqGetCookieSubValue('ELOQUA', 'GUID');
    elqCustomerGuid = elqCustomerGuid == '' ? Date.now():elqCustomerGuid;
    if (elqCustomerGuid != null && elqCustomerGuid != '')
        document.forms[0].elements['elqCustomerGUID'].value = elqCustomerGuid;
        //$("input[name='elqCustomerGUID']").val(elqCustomerGuid);

    return;
}

function setHiddenVars(){
	var currentURL = window.location.host + window.location.pathname;

    //$("input[name='Landing_Page_ID_URL1']").val(currentURL);
	document.forms[0].elements['Landing_Page_ID_URL1'].value = currentURL;

    var date = new Date();
  	var mm = ('0' + (date.getMonth() + 1)).slice(-2);
 	var dd = ('0' + date.getDate()).slice(-2);
 	var yyyy = date.getFullYear();
 	var hh = ('0' + date.getHours()).slice(-2);
 	var min = ('0' + date.getMinutes()).slice(-2);
 	var ss = ('0' + date.getSeconds()).slice(-2);
 	var formattedDate = mm + '/' + dd + '/' + yyyy + ' ' + hh + ':' + min + ':' + ss;
	document.forms[0].elements['FormSubmitDate'].value = formattedDate;
    //$("input[name='FormSubmitDate']").val(formattedDate);
}

window.onload = elqSetCustomerGUID();
window.onload = setHiddenVars();
document.onload = handleDocumentLoad('form6750', '177775138');