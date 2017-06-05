(function () {
$(document).ready(function (){
	$('#reload_btn').on('click', function(){
		var path2 = $(this).attr("path");
		load(path2);
	});

	function load(path){
		$.ajax({
			method: 'get',
			beforeSend: function(xhr){  var token = $("meta[name='csrf-token']").attr("content");
				  				xhr.setRequestHeader("X-CSRF-Token", token);},
			//url: 'data/metrics.json',
			url: path,
			dataType: 'json',
			async: true,
			complete: function(jqXHR) {
				console.log('fillData complete, jqXHR readyState is ' + jqXHR.readyState);

				if(jqXHR.readyState === 4) {

				console.log('jqXHR readyState = 4');

	       			if(jqXHR.status == 200){
						var randomBetween = function (min, max) {
						    if (min < 0) {
						        return min + Math.random() * (Math.abs(min)+max);
						    } else {
						        return min + Math.random() * max;
						    }
						};
						var str = jqXHR.responseText;
						console.log('str = ' + str);
						var json = $.parseJSON(str);
						var factors_array = [];
						var factor_json;
						var cholesterol = {
							label   : 'Total Cholesterol',
							score   : 0,
							value : 0,
							actual: 0,
							weight: 0,
							details : []
						}
						var bp = {
							label   : 'Blood Pressure',
							score   : 0,
							value : 0,
							actual: 0,
							weight: 0,
							details : []
						}					
						factor_json = json[0].metrics;
						
						console.log(factor_json);
						for (var i = 0; i < factor_json.length; i++) {
							var random = factor_json[i].value;
							console.log(factor_json[i].details);
							if (factor_json[i].details != undefined) {
								var superFactor = {
									label: factor_json[i].name,
									score: 0,
									value: 0,
									actual: 0,
									weight: 0,
									details : []
								}	
								for (var j = 0; j < factor_json[i].details.length; j++) {
									superFactor.details.push({
										label: factor_json[i].details[j].name,
										score: HGraph.prototype.calculateScoreFromValue(factor_json[i].details[j].features, random), 
										value: parseFloat(random).toFixed(0) +  ' ' +  factor_json[i].details[j].features.unitlabel,
										actual: random,
										weight: factor_json[i].details[j].features.weight
									});
								}

								for(var j = 0; j < superFactor.details.length; j++) {
									superFactor.score = superFactor.score + superFactor.details[j].score
									superFactor.actual = superFactor.actual + superFactor.details[j].actual
									superFactor.weight = superFactor.weight + superFactor.details[j].weight
								}

								superFactor.score /= j;
								superFactor.weight /= j;
								superFactor.value = parseFloat(superFactor.actual).toFixed(2)  +  ' ' + factor_json[i].features.unitlabel;
								factors_array.push(superFactor);
								superFactor = null
							}
							else 
								factors_array.push(
								{
									label: factor_json[i].name,
									score: HGraph.prototype.calculateScoreFromValue(factor_json[i].features, random), 
									value: parseFloat(random).toFixed(0) +  ' ' +  factor_json[i].features.unitlabel,
									weight: factor_json[i].features.weight
								}
							)
						}
						var opts = {
							container: document.getElementById("viz"),
							userdata: {
								hoverevents : true,
					            factors: factors_array
							},
							showLabels : true
						};
						console.log(opts);
						graph = new HGraph(opts);
						graph.width = $(window).width();
						graph.height = $(window).height();
						graph.initialize();
						$('#zoom').on('click', function(){
							console.log(graph.isZoomed)
							if (graph.isZoomed)
								graph.zoomOut();
							else
								graph.zoomIn();
						});
						
						$("#zoom_btn").click(function(){
							var s = graph.isZoomed;
							if(!s){
								$(this).find("span").addClass("zoomed");
								graph.zoomIn();
							}else{
								$(this).find("span").removeClass("zoomed");
								graph.zoomOut();
							}
						});
						
						$("#connector_btn").click(function(){
							var t = graph.toggleConnections();
							if(!t){
								$(this).find("span").addClass("toggled");
							} else {
								$(this).find("span").removeClass("toggled");
							}
						});
							
						function focusFeature( f, e ){
							
							for(var key  in graph.layers){
								var p = graph.layers[key];
								if( e == key ){ 
									p.transition()	
										.duration(120)
										.ease("cubic")
										.attr("opacity",1.0);
										continue;
								}
								p.transition()	
									.duration(120)
									.ease("cubic")
									.attr("opacity",0.1);
							}
							if(f == "points"){
								for(var i in graph[f]){
									graph[f][i]
										.transition()
										.duration(1200)
										.ease("elastic")
										.attr("r",graph.getPointRadius()*1.5);
								}
							} else {
								graph[f].transition()
									.duration(1200)
									.ease("elastic")
									.attr("transform","scale(1.5)");
							}
						};
						
						function returnToNormal( f ){
							if(f == "points"){
								for(var i in graph[f]){
									graph[f][i]
										.transition()
										.duration(1200)
										.ease("elastic")
										.attr("r",graph.getPointRadius());
								}
							} else {
								graph[f].transition()
									.duration(1200)
									.ease("elastic")
									.attr("transform","scale(1.0)");
							}
						};
						
						function returnall(){
							for(var key  in graph.layers){
								var p = graph.layers[key];
								p.transition()	
									.duration(120)
									.ease("cubic")
									.attr("opacity",1.0);
							}
						};
						
						function atEnd( which ){
							var btn = (which > 0) ? "#next_info" : "#prev_info";
							$(".novis").removeClass("novis");
							$(btn).addClass("novis");
						};
					
						function inMiddle(){
							$(".novis").removeClass("novis");
						};
					
						/* set the size of the info slider */
						var t = 4*($(window).width()*percent+40), c = 0, l = 4, iil = $("#info_panel .info_item"), percent = 0.9, percent2 = 0.4;
						$("#info_panel .info_item").css("width",($(window).width()*percent+"px"));
						$("#info_slider").css("width",(t+"px"));
						/* info slider controll button clicks */
						$(".control_btn").click(function(){
							var i  = parseInt( this.dataset.inc ),
								nc = c + i,
								fc = c + (i * 2);
							if(nc < 0 || nc > (l-1)){ return };
							if(fc < 0 || fc > (l-1)){ atEnd(fc); }
							else{ inMiddle(); }
							
							var p = iil[c].dataset.feature;
							if( p ){ returnToNormal( p ); }
							
							d3.timer.flush();
							
							c += i;
							var d = c * (-($(window).width()*percent+40));
							$("#info_slider").stop().animate({
								"left":(d+"px")
							},$(window).height()*percent2);
							
							var f = iil[c].dataset.feature,
								e = iil[c].dataset.exclude;
							if( d ){ focusFeature( f, e ); }
							else{ returnall(); }		
						});
						
						$('.graph_nav_opt').on("mousedown",function(){
							$(this).removeClass("grad1").addClass("grad2");
						}).on("mouseup",function(){
							$(this).removeClass("grad2").addClass("grad1");
						});
						//*//
					
						function closeInfo(){
							returnall();
							returnToNormal("points");
							returnToNormal("overalltxt");
							returnToNormal("ring");	
							c = 0;
							var d = c * (-($(window).width()*percent+40));
							$("#info_slider").stop().animate({
								"left":(d+"px")
							},$(window).height()*percent2);
							atEnd(c);
						};
					
						$("#info_btn").click(function(){
							var r = $(this).hasClass("risen");
							if(!r){
								$("#info_panel").stop().animate({
									"bottom" : "0px"
								},$(window).height()*percent2);
								$(this).addClass("risen");
							} else {
								$("#info_panel").stop().animate({
									"bottom" : "-"+$(window).height()*percent2+"px"
								},$(window).height()*percent2);
								closeInfo();
								$(this).removeClass("risen");
							}
						});
					}
	       		}
	    	}

		});
	};
});
})();