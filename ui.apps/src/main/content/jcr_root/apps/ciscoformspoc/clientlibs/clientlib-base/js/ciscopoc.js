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
    return;
}
window.onload = elqSetCustomerGUID;