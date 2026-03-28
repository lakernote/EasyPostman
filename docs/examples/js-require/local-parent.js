var child = require("./local-child.js");

module.exports = {
    describe: function(name) {
        return "local-parent:" + name + ":" + child.suffix;
    },
    marker: function() {
        return child.marker;
    }
};
