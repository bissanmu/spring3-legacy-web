/*
 * Main Javascript
 */


var _mediaQuery = '';
var _isMobileDevice = false;
var _minHeaderScrollOffset = 100;
var _pageScrollingWeight = 1.0;
var _isForcedScrolling = false;

vex.defaultOptions.className = "vex-theme-default";

$(document).ready(function() {
	initializeAll();
	$(window).trigger('resize');
	$(window).trigger('scroll');
});

$(window).resize(function(e) {
	updateMediaQuery();
	eqalsH();
});

$(window).scroll(function(e) {
	updateHeader(e);
	updateSpy(e);
	updateSticky(e);
});

$(window).load(function(){
	eqalsH();
});

function initMain(){	
	if ( $('#sticky-report').length != 0 ){
		$('#sticky-report').find('.title-area').find('button').click(function(e){
			e.preventDefault();
			if ( $('#sticky-report').hasClass('active') ){
				$('#sticky-report').removeClass('active');
			}else{
				$('#sticky-report').addClass('active');			
			}
		});
	}
}
function updateSpy(e){
	if ( $("#main-side-nav").length != 0 ){
		var scrollTop = $(window).scrollTop();
		var endPoint = $(document).height() - $(window).height();
		if ( $(window).scrollTop() == endPoint ) {
			$('#main-side-nav').stop().fadeOut('fast');
			//console.log('end');
		}else{
			if (_mediaQuery != "xs" || _mediaQuery != "sm"){
				if (scrollTop > 500) {
					$('#main-side-nav').stop().fadeIn('fast');
				} else {
					$('#main-side-nav').stop().fadeOut('fast');
				}
			}
		}


	}
}

function updateSticky(e){
	if ( $('#sticky-report').length != 0 ) {
		var scrollTop = $(window).scrollTop();
		var docH = $("body").height();		
		
		if (_mediaQuery != "xs" || _mediaQuery != "sm"){
			if (docH < 1700){
				$('#sticky-report').stop().fadeIn('fast');
			}else{
				if (scrollTop > 800) {
					$('#sticky-report').stop().fadeIn('fast');
				}else{
					$('#sticky-report').stop().fadeOut('fast');
				}
			}
		}else if( _mediaQuery == "xs" || _mediaQuery == "sm" ){
			if (docH < 1400){
				$('#sticky-report').stop().fadeIn('fast');
			}else{
				if (scrollTop > 500) {
					$('#sticky-report').stop().fadeIn('fast');
				}else{
					$('#sticky-report').stop().fadeOut('fast');
				}
			}
			
		}
	}
}

function initializeAll() {
	init_menu();
	init_help();
	initSliders();
	initSelectric();
	initTab();
	initBeforeAfter();
	intiScrollspy();
	placeHolder();
	initMain();
	initPrint();
	initMalihuScrollbar();
	initDatepicker();

	$('a').on("keyup",function(key){ 
		if(key.keyCode==13){
			$(this).click();
		} 
	});	

	$('input[type=file]').on("focus",function(){ 
		$(this).prev('label').css({"outline":"1px dashed red"})
	});	
	$('input[type=file]').on("blur",function(){ 
		$(this).prev('label').css({"outline":"none"})
	});	

	var lpObjTabbable = $('.modal').find("button, input:not([type='hidden']), select, iframe, textarea, [href], [tabindex]:not([tabindex='-1'])");
	var lpObjTabbableFirst = lpObjTabbable && lpObjTabbable.first();
	var lpObjTabbableLast = lpObjTabbable && lpObjTabbable.last();

	lpObjTabbableLast.on("keydown", function(event) {
		if (!event.shiftKey && (event.keyCode || event.which) === 9) {
			event.preventDefault();
			lpObjTabbableFirst.focus();
		}
	});
	
	var lpObjTabbable = $('.price_info').find("button, input:not([type='hidden']), select, iframe, textarea, [href], [tabindex]:not([tabindex='-1'])");
	var lpObjTabbableFirst = lpObjTabbable && lpObjTabbable.first();
	var lpObjTabbableLast = lpObjTabbable && lpObjTabbable.last();

	lpObjTabbableLast.on("keydown", function(event) {
		if (!event.shiftKey && (event.keyCode || event.which) === 9) {
			event.preventDefault();
			lpObjTabbableFirst.focus();
		}
	});

}

// Malihu custom scrollbar
// ----------------------------------------------
function initMalihuScrollbar() {
	$('.custom-scroll-container').mCustomScrollbar();
}

function initDatepicker(){
	$('.datepicker').datetimepicker({
		format: 'YYYY/MM/DD'
	});
}

function initPrint(){
	$('button[data-action=print]').click(function(e){
		$('html').addClass('print');
		e.preventDefault();
		window.print();
		$('html').removeClass('print');
	});
}

function eqalsH(){
	equalheight('.equal-height');
}

function placeHolder(){
	if ($('html').hasClass('ie9')){
		$('input, textarea').placeholder();
	}	
}

function intiScrollspy(){
	if ( $('#main-side-nav').length != 0 ){
		$('body').scrollspy({ 
			target: '#main-side-nav',
			offset: $('#header').height()
		});
		if ($('#main-side-nav').length != 0 ){
			$('#main-side-nav .point').click(function(e){
				e.preventDefault();
				var t = $(this).attr('href');
				moveToTab(t);
			});
		}
	}
}



function init_menu(){
	$('#header .allMenu button').click(function(e){
		e.preventDefault();
		if ( $("html").hasClass('menu_open') ){
			$("html").removeClass('menu_open');
		}else{
			$("html").addClass('menu_open');
		}
	});
}
function init_help(){
	$('#header .helpMenu button').click(function(e){
		e.preventDefault();		
		$("html").addClass('help_open');
	});
	$('.help-menu  button').click(function(e){
		e.preventDefault();
		$("html").removeClass('help_open');
		$('#header .helpMenu button').focus();
	});
	
	var lpObjTabbable = $('.hele-container').find("button, input:not([type='hidden']), select, iframe, textarea, [href], [tabindex]:not([tabindex='-1'])");
	var lpObjTabbableFirst = lpObjTabbable && lpObjTabbable.first();
	var lpObjTabbableLast = lpObjTabbable && lpObjTabbable.last();

	lpObjTabbableLast.on("keydown", function(event) {
		if (!event.shiftKey && (event.keyCode || event.which) === 9) {
			event.preventDefault();
			lpObjTabbableFirst.focus();
		}
	});
}

function updateHeader(e) {	
	var scrollTop = $(window).scrollTop();

	if (_mediaQuery != "xs" || _mediaQuery != "sm"){
		if (scrollTop > _minHeaderScrollOffset) {
			$('#header').addClass('tint');
		} else {
			$('#header').removeClass('tint');
		}
	}
}

function moveToTab(tabId) {
	var $tab = $(tabId);
	if ($tab.length != 1) {
		return;
	}
	var offset = Math.floor($tab.offset().top - $('#header').height()) + 10;
	var duration = Math.sqrt(Math.abs($(window).scrollTop() - offset)) * _pageScrollingWeight * 20.0;
	_isForcedScrolling = true;
	$('body, html').stop().animate({scrollTop : offset}, duration, function() {
		_isForcedScrolling = false;
	});
}

var _helper = {
	getMediaQuery: function() {
		var windowWidth = $(window).innerWidth();
	  var mq = 'xs';
	  if (windowWidth < 768) {
	    mq = 'xs';
	  } else if (windowWidth < 1024) {
	    mq = 'sm';
	  } else if (windowWidth < 1200) {
	    mq = 'md';
	  } else {
	    mq = 'lg';
	  }
	  return mq;
	},
	isMobileDevice: function() {
		return (typeof window.orientation !== "undefined") || (navigator.userAgent.indexOf('IEMobile') !== -1);
	}
}

function updateMediaQuery() {
	_mediaQuery = _helper.getMediaQuery();
}

function initTab(){
	$('#video-tab a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
	  e.target // newly activated tab
	  //e.relatedTarget // previous active tab
	 // console.log(e.relatedTarget);
	  var src = $(e.target).data('youtube');
	  var tgt = $(e.target).data('target')	  
	  $(tgt).find('iframe').attr('src', src);

	  var old = $(e.relatedTarget).data('target');	  
	  $(old).find('iframe').attr('src', '');
	})
}

function initBeforeAfter(){
	if ( $('#beforeAfter').length != 0 ){
		$('#beforeAfter').beforeafter({
			hide_message: false
		});
	}	
}

// Slick custom sliders
// ----------------------------------------------
var heroTimer = false;
function initSliders() {
	var _sliderInfo = {
	  prevArrow: '<button type="button" class="slick-prev"><span class="icon-font icon-angle-left"></span></button>',
	  nextArrow: '<button type="button" class="slick-next"><span class="icon-font icon-angle-right"></span></button>'
	};

	if ( $('#CH-hero-slider').length != 0 ){
		var heroSlider = $('#CH-hero-slider').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: false,
			fade: true,
			adaptiveHeight: false,
			dots: false,
			accessibility:true
		});

		$('#CH-hero-slider .round-tab').each(function(idx){
			$(this).find('a').each(function(idx1){
				$(this).click(function(e){
					e.preventDefault();
					heroSlider.slick('slickGoTo',idx1);
				});				
			});
		});

		/*$('#CH-hero-slider').on('beforeChange', function(event, slick, currentSlide, nextSlide){
			if (nextSlide == 1){
				//heroTimer = true;
				if (heroTimer){
					clearTimeout(heroTimer);
				}
				heroTimer = setTimeout(function(){
					heroSlider.slick('slickGoTo',0);
				}, 10000); //5000
			}else{
				if (heroTimer){
					clearTimeout(heroTimer);
				}
			}
		});
		$('#CH-hero-slider .slick-slide').eq(1).find('input').focus(function(){
			if (heroTimer){
				clearTimeout(heroTimer);
			}			
		});
		$('#CH-hero-slider .slick-slide').eq(1).find('input').blur(function(){
			if (heroTimer){
				clearTimeout(heroTimer);
			}			
			heroTimer = setTimeout(function(){
				heroSlider.slick('slickGoTo',0);
			}, 10000); //5000
		});*/

		$('#CH-hero-slider #menu1 a[role=button]').click(function(e){
			e.preventDefault();
			t = $(this).data('target');
			if ( !$(t).hasClass('in') ){
				$('#CH-hero-slider #menu1 .tooltip').removeClass('in');
			}
		});
		$('#CH-hero-slider #menu2 a[role=button]').click(function(e){
			e.preventDefault();
			t = $(this).data('target');
			if ( !$(t).hasClass('in') ){
				$('#CH-hero-slider #menu2 .tooltip').removeClass('in');
			}
		});
		$('#CH-hero-slider #menu3 a[role=button]').click(function(e){
			e.preventDefault();
			t = $(this).data('target');
			if ( !$(t).hasClass('in') ){
				$('#CH-hero-slider #menu3 .tooltip').removeClass('in');
			}
		});
	}

	if ( $('.slick-slider[data-type="review"]').length != 0 ){
		var reviewSlider = $('.slick-slider[data-type="review"]').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: true,
			dots: false,
			adaptiveHeight: false,
			mobileFirst: true,
			slidesToShow: 1,
			slidesToScroll: 1,
			responsive: [				
				{
				  breakpoint: 768,
				  settings: {
					slidesToShow: 2,
					slidesToScroll: 2
				  }
				},
				{
				  breakpoint: 1024,
				  settings: {
					slidesToShow: 3,
					slidesToScroll: 3
				  }
				}				
			  ]
		});
	}

	if ( $('.slick-slider[data-type="character"]').length != 0 ){
		var characterSlider = $('.slick-slider[data-type="character"]').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: true,
			dots: true,
			adaptiveHeight: true,
			mobileFirst: true,
			slidesToShow: 1,
			slidesToScroll: 1,
			responsive: [				
				{
				  breakpoint: 768,
				  settings: {
					slidesToShow: 2,
					slidesToScroll: 2
				  }
				},
				{
				  breakpoint: 1024,
				  settings: {
					arrows: false,
					slidesToShow: 4,
					slidesToScroll: 4,
					dots: false
				  }
				}				
			  ]
		});
	}

	if ( $('.slick-slider[data-type="popular"]').length != 0 ){
		popularSlider = $('.slick-slider[data-type="popular"]').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: true,
			dots: false,
			adaptiveHeight: true,
			mobileFirst: true,
			slidesToShow: 1,
			slidesToScroll: 1,
			infinite: false,
			centerMode: true,
			centerPadding: '50px',
			responsive: [				
				{
				  breakpoint: 768,
				  settings: {
					arrows: true,
					slidesToShow: 2,
					slidesToScroll: 2,
					centerMode: false
				  }
				},
				{
				  breakpoint: 1024,
				  settings: {
					arrows: false,
					centerMode: false,
					slidesToShow: 5,
					slidesToScroll: 5
				  }
				}				
			  ]
		});
	}

	if ( $('.slick-slider[data-type="install-guide"]').length != 0 ){
		popularSlider = $('.slick-slider[data-type="install-guide"]').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: true,
			dots: true,
			adaptiveHeight: true,
			mobileFirst: true,
			slidesToShow: 1,
			slidesToScroll: 1,
			infinite: false			
		});
	}
	if ( $('.slick-slider[data-type="report"]').length != 0 ){
		reportSlider = $('.slick-slider[data-type="report"]').slick({
			prevArrow: _sliderInfo.prevArrow,
			nextArrow: _sliderInfo.nextArrow,
			arrows: true,
			dots: true,
			//adaptiveHeight: true,
			mobileFirst: true,
			slidesToShow: 1,
			slidesToScroll: 1,
			infinite: true
		});
	}

	
}

function initSelectric(){
	$('.select').selectric();
}

function heroSelect(o) {
	var o = $(o);
	var val = o.val();
	var target = o.parents(".column").next('.column').find("input");
	target.addClass('hidden');
	target.val("");
	target.eq(val).removeClass('hidden');	
}

function draw_chart(obj, labels, values, ymax){
	//obj 
	
	var options = {		
		chart : {
            renderTo: obj,
            type: 'column'
        },
        title: {
            text: ''
        },
		tooltip: false,
		legend: false,
        xAxis: {
            categories: labels
        },
        yAxis: {
			min : 0,
			max : ymax,
            title: {
                text: ''
            }
        },		
        series: [{
            data: values
        }]
	};
	
	var chart = new Highcharts.Chart(options);
	$("#bar3").find(".highcharts-series-group rect").each(function(idx){
		if (idx != 0){
			$(this).removeClass("highcharts-color-0").addClass("highcharts-color-1");
		}		
	});
	$("#bar4").find(".highcharts-series-group rect").each(function(idx){
		if (idx != 0){
			$(this).removeClass("highcharts-color-0").addClass("highcharts-color-1");
		}		
	});

}

function draw_chart_bar(obj, labels, values){
	//obj 
	//console.log(values);
	var options = {		
		chart : {
            renderTo: obj,
            type: 'bar'
        },
        title: {
            text: ''
        },
		tooltip: false,
		legend: false,
        xAxis: {
            categories: labels
        },
        yAxis: {
			min : 0,
            title: {
                text: ''
            }
        },
		plotOptions: {
			bar: {
				dataLabels: {
					enabled: true
				}
			}
		},
        series: [{
            data: values
        }]
	};
	
	var chart = new Highcharts.Chart(options);
	$("#bar1").find(".highcharts-series-group rect").each(function(idx){
		if (idx != 0){
			$(this).removeClass("highcharts-color-0").addClass("highcharts-color-1");
		}		
	});
	$("#bar2").find(".highcharts-series-group rect").each(function(idx){
		if (idx != 0){
			$(this).removeClass("highcharts-color-0").addClass("highcharts-color-1");
		}		
	});

}

equalheight = function(container){
	var wW = $(window).width();


	var currentTallest = 0,
     currentRowStart = 0,
     rowDivs = new Array(),
     $el,
     topPosition = 0;
	 $(container).each(function() {

	   $el = $(this);
	   $($el).height('auto')
	   //topPostion = $el.position().top;
	   topPostion = $el.offset().top;
	 
	   if (currentRowStart != topPostion) {
		 for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
		   rowDivs[currentDiv].height(currentTallest);
		 }
		 rowDivs.length = 0; // empty the array
		 currentRowStart = topPostion;
		 currentTallest = $el.height();
		 rowDivs.push($el);
	   } else {
		 rowDivs.push($el);
		 currentTallest = (currentTallest < $el.height()) ? ($el.height()) : (currentTallest);
	  }
	   for (currentDiv = 0 ; currentDiv < rowDivs.length ; currentDiv++) {
		   if (wW < 990){
				rowDivs[currentDiv].height('auto');
		   }else{
				rowDivs[currentDiv].height(currentTallest);
		   }
	   }
 });
}

$('.modal').on('shown.bs.modal', function (e) {
  $(body).removeClass('modal-open');
  $('html').addClass('modal-open');
});
$('.modal').on('hidden.bs.modal', function (e) {
   $('html').removeClass('modal-open');
})

// Hide Console.log
// ----------------------------------------------
if (!$('html').hasClass('DEBUG')) {
  var console = {};
  console.log = function(){};
  window.console = console;
}

// 
! function(e) {
    e.fn.beforeafter = function(i) {
        var t = e.extend({
            touch: !0,
            message: "Slide",
            hide_message: !0,
            reset: !0,
            reset_delay: 3e3,
            drag_horizontal: !0,
            split_horizontal: !0
        }, i);
        return this.each(function() {
            var i = e(this),
                a = i.find("img"),
                n = a.data("aftersrc"),
                s = i.width(),
                d = 0;
            a.after('<div class="g-img-after" style="background-image:url('+n+');"></div>'),
                a.addClass("g-img-before").width(s), i.append('<div class="g-img-divider"><span><i></i></span></div>'), d = i.height(), t.split_horizontal || i.addClass("g-vertical"),
                i.on("mouseenter touchstart", function(e) {
                    var t = i.data("reset-timer");
                    t && (window.clearTimeout(t), i.data("reset-timer", !1))
                }).on("mousemove touchmove", function(a) {
                    var n = 0,
                        o = 0,
                        r = i.find(".g-img-divider span");
                    if (t.drag_horizontal) n = a.pageX - i.offset().left, o = n / s * 100;
                    else {
                        var f = i.offset().top - e(window).scrollTop();
                        n = a.clientY / f,
                            o = (a.clientY - f) / d * 100
                    }
                    if (t.touch && "undefined" != typeof a.originalEvent.touches) {
                        var g = a.originalEvent.touches[0];
                        o = t.drag_horizontal ? (g.pageX - i.offset().left) / s * 100 : (g.pageY - i.offset().top) / d * 100;
                    }
                    t.split_horizontal ? (i.find(".g-img-after").css("left", o + "%"), i.find(".g-img-divider").css("left", o + "%")) : (i.find(".g-img-after").css("top", o + "%"), i.find(".g-img-divider").css("top", o + "%")),
                        t.hide_message && r.is(":visible") && r.fadeOut()
                }).on("mouseleave touchend touchcancel", function(e) {
                    var a = i.data("reset-timer"),
                        n = i.find(".g-img-divider span");
                    t.reset && (a || (a = window.setTimeout(function() {
                        t.split_horizontal ? (i.find(".g-img-after").animate({
                                left: "50%"
                            }, 500), i.find(".g-img-divider").animate({
                                left: "50%"
                            }, 500, function() {
                                n.fadeIn()
                            })) : (i.find(".g-img-after").animate({
                                top: "50%"
                            }, 500), i.find(".g-img-divider").animate({
                                top: "50%"
                            }, 500, function() {
                                n.fadeIn()
                            })),
                            i.data("reset-timer", !1)
                    }, t.reset_delay), i.data("reset-timer", a)))
                })
        }), this
    }
}(jQuery);

function show_loading(){
	if ( $("#loading-layer").length != 0 ){
		$("#loading-layer").remove();		
	}

	var loadingHTML = "<div id='loading-layer'><div class='loading-box'><div class='table-cell align-middle text-center'>결제가 진행중 입니다.</div></div></div>";
	$("body").append(loadingHTML);
	$("#loading-layer").stop().fadeIn('fast');
}
function hide_loading(){
	$("#loading-layer").stop().fadeOut('fast',function(){
		$("#loading-layer").remove();
	})
}