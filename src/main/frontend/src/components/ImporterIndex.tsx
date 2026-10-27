import {Button, Grid, Space, Typography, Alert, message, theme} from "antd";
import {InboxOutlined, ImportOutlined} from "@ant-design/icons";
import axios from "axios";
import {ResourceDragger} from "@zrlog/ui/resources";
import ImporterResourcePicker from "./ImporterResourcePicker";
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
    const [sourceName, setSourceName] = useState("");
    const [error, setError] = useState("");
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
        setError("");
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
            setError(e instanceof Error ? e.message : "导入失败");
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
                        <ResourceDragger
                            accept="application/zip,.zip"
                            disabled={importing}
                            cardStyle={{width: "100%"}}
                            height={180}
                            onUploadingChange={setUploading}
                            beforeUpload={(file) => {
                                if (!file.name.toLowerCase().endsWith(".zip")) {
                                    setError("请选择 ZIP 文件");
                                    return false;
                                }
                                if (file.size > 64 * 1024 * 1024) {
                                    setError("ZIP 文件不能超过 64 MiB");
                                    return false;
                                }
                                setError("");
                                return true;
                            }}
                            upload={async (file, {signal, onProgress}) => {
                                const form = new FormData();
                                form.append("file", file);
                                const {data: result} = await axios.post(
                                    `${data.contextPath}/api/admin/upload?dir=hexo&temporary=true`, form,
                                    {signal, onUploadProgress: ({loaded, total}) => {
                                        if (total) onProgress(loaded / total * 100);
                                    }},
                                );
                                if (!result.data?.url || result.error || result.success === false) {
                                    throw new Error(result.message || "上传失败");
                                }
                                return {url: result.data.url, name: file.name};
                            }}
                            onSuccess={(resource) => {setSource(resource.url); setSourceName(resource.name || resource.url);}}
                            onError={(reason) => setError(reason.message)}
                            resourcePicker={{
                                title: "选择 ZIP 文件", buttonText: "从资源库选择",
                                render: (close) => <ImporterResourcePicker contextPath={data.contextPath}
                                    onSelect={(entry) => {setSource(entry.path); setSourceName(entry.name); setError(""); close();}}/>,
                            }}
                        >
                            <p className="ant-upload-drag-icon"><InboxOutlined/></p>
                            <p className="ant-upload-text">上传 Hexo Markdown ZIP 文件</p>
                            <p className="ant-upload-hint">支持打包 _posts 文件夹，最大 64 MiB</p>
                        </ResourceDragger>
                        {sourceName && <Typography.Text style={{overflowWrap: "anywhere"}}>已选择：{sourceName}</Typography.Text>}
                        {error && <Alert type="error" showIcon title={error}/>}

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
