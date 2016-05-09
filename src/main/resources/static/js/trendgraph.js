function makeTrendGraph(word) {
    var names = usersToCompare;
    var worddata = displayedSugs[word];
    var dataset = [];
    
    if (RTnotLike) {
        for (var i = 0; i < names.length; i++) {
            if (worddata.companiesRT[names[i]] !== undefined) {
                dataset.push(worddata.companiesRT[names[i]]);
            } else {
                dataset.push(0);
            }
        }
    } else {
        for (var i = 0; i < names.length; i++) {
            if (worddata.companiesLK[names[i]] !== undefined) {
                dataset.push(worddata.companiesLK[names[i]]);
            } else {
                dataset.push(0);
            }
        }
    }
    
    var margin = {top: 20, right: 20, bottom: 50, left: 50},
        width = 500
        height = 200;

    var padding = 25;
    var scale = height/Math.max.apply(null, dataset); // 4x scale

    // create svg element
    var svg = d3.select("#div-chart")
      .append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", height + margin.top + margin.bottom)
      .append('g')
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");;

    svg.selectAll("rect")
      .data(dataset)
      .enter()
      .append("rect")
      .attr("x", function(d,i){
        return i* (width/dataset.length) + padding/2;
      })
      .attr("y", function(d){ return height - d*scale; })
      .attr("width", width/dataset.length - padding)
      .attr("height", function(d){return d*scale;})
      .attr("fill", function(d){
        var r = 0;
        var g = 225;
        var b = 225;
        return "rgb(" +r+ "," +g+ "," +b+ ")";
      });

    var xScale = d3.scale.ordinal()
      .domain(names)
      .rangeBands([0, width])
    var yScale = d3.scale.linear()
      .domain([Math.max.apply(null, dataset), 0])
      .range([0, height])

    // Axis
    var xAxis = d3.svg.axis()
                  .scale(xScale)
                  .orient("bottom")
    var yAxis = d3.svg.axis()
                  .scale(yScale)
                  .orient("left")
                  .ticks(10);

    var xAxisGroup = svg.append("g")
      .attr("class", "axis") // assign "axis" class
      .attr("transform", "translate(0, " + (height) + ")")
      .call(xAxis);

    var yAxisGroup = svg.append("g")
      .attr("class", "axis")
      .call(yAxis);

    svg.append("text")
      .attr("x", width/2)
      .attr("y", height + 40)
      .style("text-anchor", "middle")
      .text("Company Name");

    svg.append("text")
      .attr("transform", "rotate(-90)")
      .attr("x", -height/2)
      .attr("y", -50)
      .attr("dy", "1em")
      .style("text-anchor", "middle")
      .text("Number of Tweets");
}