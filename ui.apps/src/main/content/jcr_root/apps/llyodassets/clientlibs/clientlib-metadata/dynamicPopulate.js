(function ($document, $) {
		//loads the code only after the page loads completely
    	$document.on("foundation-contentloaded", populateValues);


    function populateValues(){
		//Arrays of values which contain values that are to be preserved
		var agency=['101 London','Agency','Alpha Grid','Anomaly','Blue State Digital','Bridgeman','Brightsource (Cello)','Channel Graphics Communication ltd','D8','Edge','Editions','EG+','Greenhouse GroupM','Ink Copywriters Ltd','Immedia broadcast Ltd','Milestone','Occam (Amaze One)','Other','Pop eye','Proximity London','Qumu (Kulu Valley)','Rainey Kelly','TAG Europe','Tangible UK (Cello)','TGAC Holdings Ltd','The Point Organisation Ltd','The Union','TRA','Xerox Corporation','Zone','Zone Digital']
		var brand=['Bank of Wales','Cheltenham & Gloucester','CIM - Verde','GI - 3rd Parties','Lloyds TSB - Do not use','Lloyds TSB International - Do not use','Lloyds TSB Islands Retail - Do not use','Lloyds TSB Offshore Corporate - Do not use','Lloyds TSB Private Banking - Do not use','Lloyds TSB Wealth Management - Do not use','Your Marketing Design Authority','TSB','Historic']
		var product = ['Insurance','iWeb','Wealth']
		var deliverableType=['Direct Mail','In Branch','Literature','Other','Paid Digital','Website']

		
		var urlParams = new URLSearchParams(window.location.search);	//will fetch the url of the asset opened
		var pathofMetadata = urlParams.get('item')+"/jcr:content/metadata.json";	//will fetch item parameter i.e path of asset and appends the json path to it

			$.getJSON(pathofMetadata, function (data) {
			   
				$.each(data, function (index, value) {
				   if(index == "agency")
					{
						agency.forEach( function( agencyValue ) {
							var values = value.split(',');
						if ($.inArray(agencyValue, values) != -1){	//if value whoch is saved in crx is available in array of values 
							$("coral-select[name='./jcr:content/metadata/lbg:agency']").append('<coral-select-item value="'+agencyValue+'" trackingelement="" selected="">'+agencyValue+'</coral-select-item>')	//appends dropdown value to the dropdown
						}

						})
					}
					else  if(index == "lbg:brand")
					{
						brand.forEach( function( brandValue ) {

							var values = value.split(',');

						if ($.inArray(brandValue, values) != -1){
							$("coral-select[name='./jcr:content/metadata/lbg:brand']").append('<coral-select-item value="'+brandValue+'" trackingelement="" selected="">'+brandValue+'</coral-select-item>')
						}

						})
					}
					else  if(index == "product")
					{
						value.forEach( function (lbgProductValue){
							 if(product.includes(lbgProductValue)){
								$("coral-select[name='./jcr:content/metadata/lbg:product']").append('<coral-select-item value="'+lbgProductValue+'" trackingelement="" selected="">'+lbgProductValue+'</coral-select-item>')
							}
						})
					}
					else  if(index == "deliverableType")
					{

							 deliverableType.forEach( function( lbgDeliverableType ) {
								var values = value.split(',');
							if ($.inArray(lbgDeliverableType, values) != -1){
							   $("coral-select[name='./jcr:content/metadata/lbg:deliverableType']").append('<coral-select-item value="'+lbgDeliverableType+'" trackingelement="" selected="">'+lbgDeliverableType+'</coral-select-item>')
							}

						})


					}

				});
			})


    }





})(Granite.$(document), Granite.$);