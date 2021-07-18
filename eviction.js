
/** Removing gis 6/25/21: judge percentage not useful
	var map = L.map('map').setView([40, -76.25], 10);

	//background map of state: cities, roads, rivers...
	map.addLayer(new L.TileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"));

	// L.tileLayer('https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw', {
	// 	maxZoom: 18,
	// 	attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, ' +
	// 		'Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
	// 	id: 'mapbox/light-v9',
	// 	tileSize: 512,
	// 	zoomOffset: -1
	// }).addTo(map);


	// control that shows state info on hover
	var info = L.control();

	info.onAdd = function (map) {
		this._div = L.DomUtil.create('div', 'info');
		this.update();
		return this._div;
	};

	info.update = function (props) {
		this._div.innerHTML = '<h4>Lanco Pandemic Eviction Case Rates</h4>' +
			'<h5>Ratio of pandemic to pre-pandemic filings</h5>' +
		(props ?
			'<div style="margin-bottom:5px"><b>' + props.ratio + '%' + ' (pre: ' + props.pre + ' post: ' + props.post + ')</b></div>' +
			'<div>' + props.court + ' ' + props.judgeName + "</div>"
			: 'Hover over an MDJ location');
	};

	info.addTo(map);


	// get color depending on population density value
	// function getColor(d) {
	// 	return d > 17550 ? "red" : "blue";
	// 	// return d > 1000 ? '#800026' :
	// 	// 		d > 500  ? '#BD0026' :
	// 	// 		d > 200  ? '#E31A1C' :
	// 	// 		d > 100  ? '#FC4E2A' :
	// 	// 		d > 50   ? '#FD8D3C' :
	// 	// 		d > 20   ? '#FEB24C' :
	// 	// 		d > 10   ? '#FED976' :
	// 	// 					'#FFEDA0';
	// }

	function getRatioColor(d) {
		return d > 40 ? '#800026' :
				d > 35  ? '#BD0026' :
				d > 30  ? '#E31A1C' :
				d > 25  ? '#FC4E2A' :
				d > 20   ? '#FD8D3C' :
				// d > 20   ? '#FEB24C' :
				// d > 10   ? '#FED976' :
							'#FFEDA0';
	}

	// function style(feature) {
	// 	return {
	// 		weight: 2,
	// 		opacity: 1,
	// 		color: 'white',
	// 		dashArray: '3',
	// 		fillOpacity: 0.7,
	// 		fillColor: getColor(feature.properties.ZCTA)
	// 	};
	// }

	function bundleProps(layer) {
		return {
			court: layer.feature.properties.Court,
			judgeName: layer.feature.properties.JudgeName,
			ratio: layer.feature.properties.courtData.ratio,
			pre: layer.feature.properties.courtData.pre,
			post: layer.feature.properties.courtData.post
		}
	}

	function highlightFeature(e) {
		var layer = e.target;

		layer.setStyle({
			weight: 5,
			color: '#666',
			dashArray: '',
			fillOpacity: 0.7
		});

		if (!L.Browser.ie && !L.Browser.opera && !L.Browser.edge) {
			layer.bringToFront();
		}

		info.update(bundleProps(layer));
	}

	var geojson;

	function resetHighlight(e) {
		geojson.resetStyle(e.target);
		info.update();
	}

	function zoomToFeature(e) {
		map.fitBounds(e.target.getBounds());
	}

	function onEachFeature(feature, layer) {
		layer.on({
			mouseover: highlightFeature,
			mouseout: resetHighlight,
			click: zoomToFeature
		});
	}

	// geojson = L.geoJson(lancasterZips,
	// {
	// 	style: style,
	// 	//onEachFeature: onEachFeature
	// }
	// ).addTo(map);

	L.geoJson(lancasterMunicipalities2,
	// {
	// 	style: style,
	// 	onEachFeature: onEachFeature
	// }
	).addTo(map);

	let baseRatio = 10;
	let baseArea = Math.pow(baseRatio, 2) * Math.PI;

	function getPointStyle(feature) {
		const ratio = feature.properties.courtData.ratio;

		let percentIncreaseOverBaseRatio = ratio/baseRatio;
		let area = baseArea * percentIncreaseOverBaseRatio;
		let radius = Math.sqrt(area/Math.PI);

		return {
		    radius: Math.max(5, radius),
		    fillColor: getRatioColor(ratio),
		    color: "#000",
		    weight: 1,
		    opacity: 1,
		    fillOpacity: 0.8
		};
	};

	function setUpCircle(feature, layer) {
		layer.bindPopup(feature.properties.Court + " " + feature.properties.JudgeName);
		layer.on({
			mouseover: highlightFeature,
			mouseout: resetHighlight
		});
	}

	//bind current court ratio/pre/post to static mdj geo info
	lancasterMdjs.features.forEach(function(item, index) {
		let court = item.properties.Court;
		let courtInfo = courtData.get(court);
		item.properties.courtData = courtInfo;
	});

	geojson = L.geoJson(lancasterMdjs,
	{
    	pointToLayer: function (feature, latlng) {
        	return L.circleMarker(latlng, getPointStyle(feature));
    	},
    	onEachFeature: setUpCircle
	}
	).addTo(map);

	map.attributionControl.addAttribution('Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenMapData</a>');


	var legend = L.control({position: 'bottomright'});

	legend.onAdd = function (map) {

		var div = L.DomUtil.create('div', 'info legend'),
			grades = [0, 20, 25, 30, 35, 40],
			labels = [],
			from, to;

		for (var i = 0; i < grades.length; i++) {
			from = grades[i];
			to = grades[i + 1];

			labels.push(
				'<i style="background:' + getRatioColor(from + 1) + '"></i> ' +
				from + (to ? '&ndash;' + to : '+'));
		}

		div.innerHTML = labels.join('<br>');
		return div;
	};

	legend.addTo(map);
**/

function initEvictions () {

//******** LANCO EVICTIONS *********

	$("#allExcel").html('All Lanco eviction cases <a href="webdata/' + pandemicDates.allName + '">1/1/15 to ' + pandemicDates.pandemicEndSlashes + "</a>");
	$("#postExcel").html('Lanco pandemic eviction cases (' +
		pandemicDates.pandemicDays + ' days) <a href="webdata/' +
		pandemicDates.postName + '">' +
		pandemicDates.pandemicStartSlashes + ' to ' +
		pandemicDates.pandemicEndSlashes + "</a>");
	$("#preExcel").html('Corresponding pre-pandemic Lanco eviction cases (' + pandemicDates.pandemicDays + ' days) <a href="webdata/' + pandemicDates.preName + '">' + pandemicDates.prePandemicStartSlashes + ' to ' + pandemicDates.prePandemicEndSlashes + "</a>");

//******** NEARBY COUNTIES **********

	$("#York").html('York eviction cases <a href="webdata/York/' +
		'York.xlsx">1/1/19 to ' +
		//pandemicDates.pandemicEndSlashes + '</a>');
		'7/18/21' + '</a>');

	$("#Lebanon").html('Lebanon eviction cases <a href="webdata/Lebanon/' +
		'Lebanon.xlsx">1/1/19 to ' +
		//pandemicDates.pandemicEndSlashes + '</a>');
		'7/18/21' + '</a>');

	$("#Berks").html('Berks eviction cases <a href="webdata/Berks/' +
		'Berks.xlsx">1/1/19 to ' +
		//pandemicDates.pandemicEndSlashes + '</a>');
		'7/18/21' + '</a>');

	$("#Dauphin").html('Dauphin eviction cases <a href="webdata/Dauphin/' +
		'Dauphin.xlsx">1/1/19 to ' +
		//pandemicDates.pandemicEndSlashes + '</a>');
		'7/18/21' + '</a>');

//******** Lancaster Statistics ************

//top 10 pandemic filers


}