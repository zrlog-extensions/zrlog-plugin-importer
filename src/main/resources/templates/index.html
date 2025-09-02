<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>${_plugin.name} - V${_plugin.version}</title>

    <link rel="stylesheet" href="assets/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="assets/css/font-awesome.min.css"/>
    <link href="assets/css/custom.min.css" rel="stylesheet"/>
    <link href="assets/css/custom.colorful.css" rel="stylesheet"/>

    <script src="assets/js/jquery-2.0.3.min.js"></script>
    <script src="assets/js/bootstrap.min.js"></script>

    <script>
        $(function () {
            $('#fileUpload').on("change", function () {
                $.ajax({
                    url: '../upload?ext=zip',
                    type: 'POST',

                    data: new FormData($('#uploadForm')[0]),

                    cache: false,
                    contentType: false,
                    processData: false,
                    success: function (e) {
                        $("#url").val(e.url);
                    }
                });
            })

            $("#doImport").click(function () {
                $.post("doImport", {"source": $("#url").val()}, function () {
                    alert("开始成功");
                })
            })
        })
    </script>

    <style>
        .page-header {
            margin: 0 0 20px;
        }

        input[type="file"] {
            display: none;
        }

        .custom-file-upload {
            border: 1px solid #ccc;
            display: inline-block;
            padding: 6px 12px;
            cursor: pointer;
        }

        .form-control {
            height: 36px;
        }
    </style>
</head>
<body style="background:#f7f7f7">
<div class="row">
    <div class="col-md-8">
        <div class="page-header">
            <h3>${_plugin.name}</h3>
        </div>
        <form id="uploadForm">
            <div class="form-group row">
                <label class="col-md-3 control-label no-padding-right"> hexo md文件（打包为zip文件） </label>
                <div class="col-md-9">
                    <input id="url" class="col-md-10 form-control">
                    <label for="fileUpload" class="custom-file-upload"> 上传 </label>
                    <input id="fileUpload" accept="application/zip" type="file" class="col-md-5 fileUpload" name="file"
                           value="">
                </div>
            </div>
        </form>
        <hr/>
        <div class="col-md-offset-3 col-md-9 row">
            <button class="btn btn-info" type="button" id="doImport">导入</button>
        </div>
    </div>
</div>
</body>
</html>