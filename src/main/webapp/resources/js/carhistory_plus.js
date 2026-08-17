//달력
$(function(){
    $(".calendar").datepicker();
});

   $.datepicker.setDefaults({
       dateFormat: 'yy.mm.dd',
       prevText: '이전 달',
       nextText: '다음 달',
       monthNames: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
       monthNamesShort: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
       dayNames: ['일', '월', '화', '수', '목', '금', '토'],
       dayNamesShort: ['일', '월', '화', '수', '목', '금', '토'],
       dayNamesMin: ['일', '월', '화', '수', '목', '금', '토'],
       showMonthAfterYear: true,
       yearSuffix: '년'
   });

   // 모달 팝업
   document.addEventListener("DOMContentLoaded", function(){
      const buttons = document.querySelectorAll("button[data-modal]");
      const modals = document.querySelectorAll(".modal");
      const closeButtons = document.querySelectorAll(".modalClose");
      const body = document.querySelector("html");
      
      for(var i=0; i<buttons.length;i++){
    	  buttons[i].addEventListener("click", function(event){
    		  const modalName = event.target.dataset.modal;
    		  
    		  if(modalName == "price_info"){	
    			  event.target.classList.add("thisClick");
    		  }
    		  
    		  if (event.target.dataset.modal === modalName) {
    			  for(j=0;j<modals.length;j++){
    				  if(modals[j].classList.contains(modalName)) {
    					  modals[j].classList.add("modalActive")
    					  modals[j].setAttribute('tabindex','0');
    					  modals[j].focus();
    				  }
    			  };
    		  };
    		  body.style.overflow='hidden';
	              
    		  var lpObjTabbable = $('.modalActive').find("button, input:not([type='hidden']), select, iframe, textarea, [href], [tabindex]:not([tabindex='-1'])");
    		  var lpObjTabbableFirst = lpObjTabbable && lpObjTabbable.first();
    		  var lpObjTabbableLast = lpObjTabbable && lpObjTabbable.last();
	   			
    		  lpObjTabbableLast.on("keydown", function(event) {
    			  if (!event.shiftKey && (event.keyCode || event.which) === 9) {
    				  event.preventDefault();
    				  lpObjTabbableFirst.focus();
    			  }
    		  });
    	  });
      };

      for(i=0;i<closeButtons.length;i++){
    	  closeButtons[i].addEventListener("click", function(event){
    		  const modal = event.target.parentElement;
    		  const modalp = modal.parentElement.parentElement;
              
    		  modalp.classList.remove("modalActive");
    		  modalp.removeAttribute("tabindex");
    		  body.style.overflow='visible';
    		  
    		  if(modalp.classList.contains("price_info")){
    			  document.querySelector(".thisClick").nextElementSibling.focus();
    			  document.querySelector(".thisClick").classList.remove("thisClick");
    			  
    		  }
    	  });
      };
   });