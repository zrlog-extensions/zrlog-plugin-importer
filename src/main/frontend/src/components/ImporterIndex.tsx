import {Button, Grid, Space, Typography, Upload, message, theme} from "antd";
import {InboxOutlined, ImportOutlined} from "@ant-design/icons";
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import {ImporterInfoResponse, StandardResponse} from "../index";

type ImporterIndexProps = {
    data: ImporterInfoResponse;
}

const ImporterIndex: FunctionComponent<ImporterIndexProps> = ({data}) => {
    const {token} = theme.useToken();
    const screens = Grid.useBreakpoint();
    const isPhone = Boolean(screens.xs && !screens.sm);
    const isCompact = !screens.md;
    const [source, setSource] = useState("");
    const [uploading, setUploading] = useState(false);
    const [importing, setImporting] = useState(false);
    const [messageApi, contextHolder] = message.useMessage();

    const shellStyle = useMemo(() => ({
        maxWidth: 980,
        margin: "0 auto",
        padding: isPhone ? 12 : isCompact ? 16 : 24,
        color: token.colorText,
        boxSizing: "border-box" as const,
    }), [isCompact, isPhone, token]);

    const panelStyle = useMemo(() => ({
        padding: isPhone ? 16 : 24,
        border: `1px solid ${token.colorBorderSecondary}`,
        borderRadius: 8,
        background: token.colorBgContainer,
    }), [isPhone, token]);

    const runImport = async () => {
        if (!source) {
            messageApi.warning("请先上传 zip 文件");
            return;
        }
        setImporting(true);
        try {
            const {data: response} = await axios.post<StandardResponse<{ count: number }>>(
                "doImport",
                new URLSearchParams({source}),
                {headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"}},
            );
            if (!response.success) {
                throw new Error(response.message || "导入失败");
            }
            messageApi.success(`已导入 ${response.data.count || 0} 篇文章`);
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "导入失败");
        } finally {
            setImporting(false);
        }
    };

    return (
        <div style={shellStyle}>
            {contextHolder}
            <Space direction="vertical" size={20} style={{width: "100%"}}>
                <Space direction="vertical" size={4}>
                    <Typography.Title level={3} style={{margin: 0, fontSize: isPhone ? 20 : undefined}}>{data.plugin.name}</Typography.Title>
                    <Typography.Text type="secondary" style={{display: "block", maxWidth: "100%"}}>{data.plugin.desc}</Typography.Text>
                </Space>

                <div style={panelStyle}>
                    <Space direction="vertical" size={20} style={{width: "100%"}}>
                        <Upload.Dragger
                            name="file"
                            accept="application/zip,.zip"
                            maxCount={1}
                            showUploadList={source ? {showRemoveIcon: false} : true}
                            action="../upload?ext=zip"
                            disabled={uploading || importing}
                            onChange={({file}) => {
                                setUploading(file.status === "uploading");
                                if (file.status === "done") {
                                    const response = file.response || {};
                                    const url = response.url || response.data?.url || "";
                                    if (url) {
                                        setSource(url);
                                        messageApi.success("上传成功");
                                    } else {
                                        messageApi.error("上传响应缺少文件地址");
                                    }
                                }
                                if (file.status === "error") {
                                    setUploading(false);
                                    messageApi.error("上传失败");
                                }
                            }}
                        >
                            <p className="ant-upload-drag-icon">
                                <InboxOutlined/>
                            </p>
                            <p className="ant-upload-text">上传 Hexo Markdown zip 文件</p>
                            <p className="ant-upload-hint">导入会按文章发布时间顺序写入 ZrLog</p>
                        </Upload.Dragger>

                        <Button
                            type="primary"
                            icon={<ImportOutlined/>}
                            onClick={runImport}
                            loading={importing}
                            disabled={!source || uploading}
                            style={isPhone ? {width: "100%"} : undefined}
                        >
                            导入
                        </Button>
                    </Space>
                </div>
            </Space>
        </div>
    );
};

export default ImporterIndex;
