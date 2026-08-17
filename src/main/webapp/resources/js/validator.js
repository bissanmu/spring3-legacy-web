$(function(){
	var id_txt = "";
	var nm_txt = "";
	var id_tel = "";
	var id_email = "";
	
	$(".nm_txt").each(function(){
		nm_txt = $(this).text();
		if(nm_txt != ""){
			nm_txt = $(this).text().substring(0,1)+"*"+$(this).text().substring(2);
		}
		$(this).text(nm_txt);
	});
	
	$(".id_txt").each(function(){
		id_txt = $(this).text();
		if(id_txt != ""){
			id_txt = $(this).text().substring(0,$(this).text().length -2)+"**";
		}
		$(this).text(id_txt);
	});
	
	$(".id_tel").each(function(){
		id_tel = $(this).text();
		if(id_tel != ""){
			id_tel = $(this).text().substring(0,$(this).text().length -4)+"****";
		}
		$(this).text(id_tel);
	});
	
	$(".id_email").each(function(){
		id_email = $(this).text();
		if(id_email != ""){
			var len1 = id_email.indexOf("@");
			id_email = $(this).text().substring(0, len1 -2)+"**"+$(this).text().substring(len1);
		}
		$(this).text(id_email);
	});
	
	
});

jQuery.fn.checkDigit = function() {
	var bool = true;
	this.each(function() {
		var self = this;
		
		var val = $(this).val();
	    var regExp = /\D/g;

	    if(regExp.test(val)) {
	        alert("숫자만을 입력하셔야 합니다.");
	        $(self).select();
	        bool = false;
	        return false;
	    }
	});
	
	return bool;
};

function isEmptyValue(value){
	return (value == null || value.length === 0);
};

//로그인 숫자,영문,특수기호 유효성 체크
function checkPass(str){ 
	var chk_num = str.search(/[0-9]/g); 
	var chk_eng = str.search(/[a-z]/ig); 
	var chk_giho = str.search(/[~,`,!,@,#,$,%,^,&,*,(,),-,_,=,+]/g);
	
	if(chk_num < 0 || chk_eng < 0 || chk_giho < 0){ 
		return false;
	} else {
		return true;
	}
}

//이메일 입력 유효성 체크
function checkEmail(str){
	var chk_email = /^([\w-\.]+)@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.)|(([\w-]+\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\]?)$/.test(str);
	
	if(chk_email){
		return true;		
	}else{
		return false;
	}
}

//숫자만 입력 유효성 체크
function checkNum(str){
	var pattern_num = /(^[0-9]+$)/;
	
	if(pattern_num.test(str)){
		return true;
	}else{
		alert("숫자만 입력하셔야 합니다.");
		return false;
	}
}

//한글만 입력 유효성 체크
function checkHan(str){
	var pattern_num = /(^[가-힣]+$)/;
	
	if(pattern_num.test(str)){
		return true;
	}else{
		alert("한글만 입력하셔야 합니다.");
		return false;
	}
}

//아이디 유효성 체크
function checkId(str){
	var pattern = /^[0-9a-zA-Z]+$/;
	
	if(str.length < 6 || str.length > 12) {
		alert("아이디는 최소 6자리 이상 12자리이하로 입력하셔야 합니다.");
		return false;
	}
	
	if(!pattern.test(str)) {
		alert('허용된 문자가 아닙니다. 다시 입력해 주십시오.');		
		return false;
	}
	
	return true;
}

//비밀번호 유효성 체크
function checkPW(str){
	var pattern_num = /([0-9])/;
	var pattern_eng = /([a-zA-Z])/;
	var pattern_giho = /([~,`,!,@,#,$,%,^,&,*,(,),-,_,=,+])/;
	
	if(str.length < 8 || str.length > 12){
		alert("8~12자리 영문/숫자/특수문자(!,@,#,$,%,^ 등) 조합만 가능합니다.");
		return false;
	}
	
	if(!pattern_num.test(str)||!pattern_eng.test(str)||!pattern_giho.test(str)||str.length < 8|| str.length > 12){
		alert("8~12자리 영문/숫자/특수문자(!,@,#,$,%,^ 등) 조합만 가능합니다.");
		return false;
	}
	
	var equalCnt = 0; //동일문자 카운트
	var pContinue = 0; //연속성(+) 카운트
	var mContinue = 0; //연속성(-) 카운트
	
	var chrFirst; //비교할 첫번째 글자
	var chrSecond; //비교할 두번째 글자
	var chrThird; //비교할 세번째 글자
	
	for(var i=0; i<str.length; i++){
		if(i >= 2){
			chrFirst = str.charCodeAt(i-2);
			chrSecond = str.charCodeAt(i-1);
			chrThird = str.charCodeAt(i);
			
			//동일문자 카운트
			if((chrFirst == chrSecond) && (chrSecond == chrThird)){
				equalCnt++;
			}else{
				equalCnt=0;
			}
			
			//연속성(+) 카운트
			if((chrFirst - chrSecond == 1) && (chrSecond - chrThird == 1)){
				pContinue++;
			}else{
				pContinue=0;
			}
			
			//연속성(-) 카운트
			if((chrFirst - chrSecond == -1) && (chrSecond - chrThird == -1)){
				mContinue++;
			}else{
				mContinue=0;
			}
			
			if(equalCnt > 0){
				alert("동일문자를 3자 이상 연속 입력할 수 없습니다.");	
				return false;
				break;
			}

			if(pContinue > 0 || mContinue > 0){
				alert("영문, 숫자는 3자 이상 연속 입력할 수 없습니다.");	
				return false;
				break;
			}
		}
	}
	
	return true;
}


//마지막 날짜 계산
function getLastDay(year, month){
	var lastDay = new Array(31,28,31,30,31,30,31,31,30,31,30,31);
	if(year %4 == 0 && year % 100 != 0 || year % 400 == 0){
		lastDay[1] = 29;
	}
	
	return lastDay[month-1];
}

// 금액을 콤마(,)가 포함된 포맷으로 변환한다.
function getCurrencyValue(n) {
	n = parseFloat(n);
	var reg = /(^[+-]?\d+)(\d{3})/;   // 정규식
	n += '';                          // 숫자를 문자열로 변환

	while (reg.test(n))
		n = n.replace(reg, '$1' + ',' + '$2');

	return n;
}

function checkDevice(){
	var isie=(/msie/i).test(navigator.userAgent); //ie
	
	var isie6=(/msie 6/i).test(navigator.userAgent); //ie 6

	var isie7=(/msie 7/i).test(navigator.userAgent); //ie 7

	var isie8=(/msie 8/i).test(navigator.userAgent); //ie 8

	var isie9=(/msie 9/i).test(navigator.userAgent); //ie 9

	var isfirefox=(/firefox/i).test(navigator.userAgent); //firefox

	var isapple=(/applewebkit/i).test(navigator.userAgent); //safari,chrome
	
	var issafari=(/safari/i).test(navigator.userAgent); //safari

	var isopera=(/opera/i).test(navigator.userAgent); //opera

	var isios=(/(ipod|iphone|ipad)/i).test(navigator.userAgent);//ios

	var isipad=(/(ipad)/i).test(navigator.userAgent);//ipad

// 	var isandroid=(/android/i).test(navigator.userAgent);//android
	
//	var isandroid=(/MobileApp/i).test(navigator.userAgent);//android
	
	var isandroid=(/CARHISTORY_APP/i).test(navigator.userAgent);//android
	
	var device;
	
	if(isios || isipad){
		//device = "iphone"; // 아이폰 앱인지 웹앱인지 정확히 구분이 안돼서 그냥 무조건 웹앱으로 인식하게 해당 소스 주석
		device = "web";
		if(issafari){
			device = "web";
		}
    }else if(isandroid){
		device = "android";
	}else{
		device = "web";
	}
	
	return device;
}

var isMobile = {
	Android: function() {
		return navigator.userAgent.match(/Android/i);
	},
	BlackBerry: function() {
		return navigator.userAgent.match(/BlackBerry/i);
	},
	iOS: function() {
		return navigator.userAgent.match(/iPhone|iPad|iPod/i);
	},
	Opera: function() {
		return navigator.userAgent.match(/Opera Mini/i);
	},
	Windows: function() {
		return navigator.userAgent.match(/IEMobile/i);
	},
	any: function() {
		return (isMobile.Android() || isMobile.BlackBerry() || isMobile.iOS() || isMobile.Opera() || isMobile.Windows());
	}
};

/**
 * @alias 문자열 바이트 수 알아내기
 * @param str
 * @return
 */
function getByteLength(str) {
	
	var temp = str.length;
	var tcount = 0;
	var onechar;
	
	for(i=0; i<temp; i++) {
		onechar = str.charCodeAt(i);
		if(onechar > 128) {
			tcount += 2;
		} else {
			tcount++;
		}
	}
	
	return tcount;
}

/**
 * @alias IOS WebView check
 * @return
 */
function getIosBrowser() {
	
	var isWebView = /(iPhone|iPod|iPad).*AppleWebKit(?!.*Safari)/i.test(navigator.userAgent);
	var str;
	
	if(isWebView) {
		// Hybrid App
		str = "APP";
	} else {
		// Web Browser(Safari)
		str = "WEB";
	}
	
	return str;
}

/**
 * @alias Android WebView check
 * @return
 */
function getAndroidBrowser() {
	
	var isWebView = /(android).*AppleWebKit(?!.*msie|firefox|applewebkit|opera)/i.test(navigator.userAgent);
	var str;
	
	if(isWebView) {
		// Hybrid App
		str = "APP";
	} else {
		// Web Browser
		str = "WEB";
	}
	
	return str;
}