
if (Function.prototype.name === undefined){
    // Add a custom property to all function values
    // that actually invokes a method to get the value
    Object.defineProperty(Function.prototype,'name',{
        get:function(){
            return /function ([^(]*)/.exec( this+"" )[1];
        }
    });
}

var getUrlParameter = function(sParam) {
    var sPageURL = window.location.search.substring(1),
        sURLVariables = sPageURL.split('&'),
        sParameterName,
        i;

    for (i = 0; i < sURLVariables.length; i++) {
        sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return typeof sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
        }
    }
    return false;
};

///////////////////////     SIMPLE GRID

class SimpleGrid{
    data;
    tableId;
    idField;
    fields;
    deleteFunction;
    saveFunction;
    objType;
    loadFunction;
    load(){
        this.loadFunction(this);
        return this;
    }
    constructor(objType,name,idField,fields,loadFunction,editFunction,deleteFunction,saveFunction){
        this.objType= objType;
        this.tableId = name;
        this.editFunction = editFunction;
        this.deleteFunction = deleteFunction;
        this.saveFunction = saveFunction;
        this.loadFunction = loadFunction;
        this.data = [];
        this.idField = idField;
        this.fields = fields;
    }
    deleteFromTable(id,callback) {
        var action = confirm("Are you sure you want to delete this "+this.objType+"?");
        var msg = this.objType+" deleted successfully!";
        var msgError = this.objType+" cannot be deleted!";
        var self = this;
        this.data.forEach(function(row, i) {
            if (row[self.idField] == id && action != false) {
                if(callback==null){
                    self.data.splice(i, 1);
                    $("#" + self.tableId + " #" + self.tableId + "-" + id).remove();
                    flashMessage(msg);
                }else {
                    callback(id, function () {
                        self.data.splice(i, 1);
                        $("#" + self.tableId + " #" + self.tableId + "-" + id).remove();
                        flashMessage(msg);
                    }, function () {
                        flashMessage(msgError);
                    });
                }
            }
        });
    }
    appendToTable(inputData){
        var idContent=inputData[this.idField];
        for(var i=0;i<this.data.length;i++){
            var line = this.data[i];
            if(line[this.idField]==inputData[this.idField]){
                this.data[i]=inputData;
                for(var v=0;v<this.fields.length;v++){
                    var index = this.fields[v];
                    var content = inputData[index];
                    if(content == undefined) content="";
                    if(content.length>60)content= content.substr(0,60);
                    $("#" + this.tableId +
                        " #" + this.tableId + "-" + idContent+
                        " #"+index).innerHTML = content;
                }
                return;
            }
        }
        this.data.push(inputData);
        var toWrite = `
        <tr id="${this.tableId}-${idContent}">`;

        /*for(var i=0;i<fields.length;i++){
            var index = fields[i];
            var content = inputData[index];
            data+=`<td class="userData" name="${index}">${content}</td>`;
        }*/

        for(var i=0;i<this.fields.length;i++){
            var index = this.fields[i];
            var allIndex = this.fields[i].split(".");
            var content = inputData;
            for(var s=0;s<allIndex.length;s++){
                content = content[allIndex[s]];
            }
            if(content == undefined) content="";
            if(content.length>60)content= content.substr(0,60);
            toWrite+=`<td class="userData" name="${index}">${content}</td>`;
        }

        toWrite+=`<td align="center">
                <button class="btn btn-success form-control" id="${this.tableId}-${idContent}-edit">EDIT</button>
            </td>
            <td align="center">
                <button class="btn btn-danger form-control" id="${this.tableId}-${idContent}-delete">DELETE</button>
            </td>
        </tr>
    `;
        var self = this;
        $("#"+this.tableId+" > tbody:last-child").append(toWrite);
        $("#"+this.tableId+"-"+idContent+"-edit").click(function(){ self.editFunction(self,idContent); });
        $("#"+this.tableId+"-"+idContent+"-delete").click(function(){ self.deleteFunction(self,idContent); });
    }
}

///////////////////////     KVP


function buildKvpModalDialog(modal, table, value, idField, valueField, randomId) {
    //var encodedValue = value[valueField].replace('"','\\"');
    $(modal).find(".modal-title").empty().append(`${table.objType}`);
    var readonly="readonly";
    if(value[idField]==''){
        readonly="";
    }
    var bodyContent = `
                <form id="editKvp" action="">
                    <label for="key">Key</label>
                    <input class="form-control" type="text" name="key" id="key" ${readonly} value="${value[idField]}"/>
                `;
    if (value[valueField].length > 60) {
        bodyContent += `
                    <label for="value">Value</label>
                    <textarea class="form-control" rows="6" cols="50" name="value" id="value" />
                `;
    } else {
        bodyContent += `
                    <label for="value">Value</label>
                    <input class="form-control" type="text" name="value"  id="value" />
                `;
    }
    $(modal).find(".modal-body").empty().append(bodyContent);

    $(modal).find(".modal-footer").empty().append(`
                    <button type="button" type="submit" class="btn btn-primary" id="${randomId}" >Save changes</button>
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                </form>
            `);
    $(modal).modal("toggle");
}

var addKvp=function(modal,table,idField,valueField) {
    var randomId = "BUTTON"+Math.floor(Math.random() * 999999999);
    var value={};
    value[idField]='';
    value[valueField]='';
    buildKvpModalDialog(modal, table, value, idField, valueField, randomId);
    var localTable = table;
    $(modal).find("#"+randomId).click(function(){
        value[idField]=$(modal).find("#key").val();
        value[valueField]=$(modal).find("#value").val();
        localTable.saveFunction(localTable,value); });
    $(modal).find("#value").val(value[valueField]);
}

var editKvp=function(modal,table,id,idField,valueField) {
    var randomId = "BUTTON"+Math.floor(Math.random() * 999999999);
    var randomIdInput = "INPUT"+Math.floor(Math.random() * 999999999);
    var localTable = table;
    localTable.data.forEach(function(value, i) {
        if (value[idField] == id) {
            buildKvpModalDialog(modal, table, value, idField, valueField, randomId);
            $(modal).find("#"+randomId).click(function(){ localTable.saveFunction(localTable,id); });
            $(modal).find("#value").val(value[valueField]);
        }
    });
}
var getKvpData = function(table,idField,valueField){
    var result = {};
    for(var i=0;i<table.data.length;i++){
        result[table.data[i][idField]]=table.data[i][valueField];
    }
    return result;
}
var updateKvp=function(modal,table,id,idField,valueField) {
    var msg = table.objType+" updated successfully!";

    if(id[idField] == undefined) {
        var user = {};
        user[idField] = id;
        table.data.forEach(function (user, i) {
            if (user[idField] == id) {
                $(modal).find("#editKvp").children("input").each(function () {
                    var value = $(this).val();
                    var attr = $(this).attr("name");
                    if (attr == idField) {
                        user[idField] = value;
                    } else if (attr == valueField) {
                        user[valueField] = value;
                    }
                });
                $(modal).find("#editKvp").children("textarea").each(function () {
                    var value = $(this).val();
                    var attr = $(this).attr("name");
                    if (attr == idField) {
                        user[idField] = value;
                    } else if (attr == valueField) {
                        user[valueField] = value;
                    }
                });
                table.data.splice(i, 1);
                table.data.splice(user[idField] - 1, 0, user);
                $("#" + table.tableId + " #" + table.tableId + "-" + user[idField]).children(".userData").each(function () {
                    var attr = $(this).attr("name");
                    var value = $(this).val();
                    if (attr == idField) {
                        $(this).text(user[idField]);
                    } else if (attr == valueField) {
                        var content = user[valueField];
                        if (content == undefined) content = "";
                        if (content.length > 60) content = content.substr(0, 60);
                        $(this).text(content);
                    }
                });
                $(modal).modal("toggle");
                flashMessage(msg);
            }
        });
    }else{
        var line = {key:id[idField],value:id[valueField]};
        table.appendToTable(line);
        $(modal).modal("toggle");
        flashMessage(msg);
    }
}


var deleteKvp=function(modal,table,id,idField,valueField) {
    table.deleteFromTable(id,null);
}


///////////////////////     FLASH MESSAGE

var flashMessage = function(msg) {
    $(".flashMsg").remove();
    $(".row").prepend(`
        <div class="col-sm-12"><div class="flashMsg alert alert-success alert-dismissible fade in" role="alert"> <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">×</span></button> <strong>${msg}</strong></div></div>
    `);
}

var setChecked = function(jqueryObj,checked){
    if(checked){
        jqueryObj.attr('checked', 'checked');
    }else{
        jqueryObj.removeAttr('checked');
    }
    jqueryObj.attr('value', checked)
}

var  downloadFile= function(urlToSend) {
    var req = new XMLHttpRequest();
    req.open("GET", urlToSend, true);
    req.responseType = "blob";
    req.onload = function (event) {
        var blob = req.response;
        var fileName = req.getResponseHeader("fileName") //if you have the fileName header available
        var link=document.createElement('a');
        link.href=window.URL.createObjectURL(blob);
        link.download=fileName;
        link.click();
    };

    req.send();
}