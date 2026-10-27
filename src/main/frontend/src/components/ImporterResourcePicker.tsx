import {ResourcePicker} from "@zrlog/ui/resources";
import type {ResourceEntry} from "@zrlog/ui/resources";
import axios from "axios";
import {useCallback, useEffect, useState} from "react";

type Entry = ResourceEntry & {actions: string[]};

export default function ImporterResourcePicker({contextPath, onSelect}: {
    contextPath: string; onSelect: (entry: Entry) => void;
}) {
    const [path, setPath] = useState("");
    const [search, setSearch] = useState("");
    const [view, setView] = useState<"grid" | "list">("list");
    const [data, setData] = useState<{entries: Entry[]; shortcuts: Entry[]}>({entries: [], shortcuts: []});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>();
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        setLoading(true);
        setError(undefined);
        axios.get(`${contextPath}/api/admin/file-manager`, {
            params: search ? {key: search} : {path}, signal: controller.signal,
        }).then(({data: result}) => {
            if (controller.signal.aborted) return;
            if (result.error || result.success === false || !result.data) throw new Error(result.message || "资源读取失败");
            setData(result.data);
        }).catch((reason) => {
            if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : "资源读取失败");
        }).finally(() => {if (!controller.signal.aborted) setLoading(false);});
        return () => controller.abort();
    }, [contextPath, path, search, retry]);
    const setSearchValue = useCallback((value: string) => {setSearch(value); if (value) setPath("");}, []);
    return <ResourcePicker
        entries={(data.entries ?? []).filter((entry) => entry.type === "directory" || entry.name.toLowerCase().endsWith(".zip"))}
        roots={data.shortcuts ?? []} path={path} search={search} viewMode={view}
        loading={loading} error={error} onRetry={() => setRetry((value) => value + 1)}
        labels={{title: "资源库", searchPlaceholder: "搜索 ZIP 文件", empty: "此目录没有 ZIP 文件",
            searchEmpty: "没有找到 ZIP 文件", grid: "网格", list: "列表", retry: "重新加载"}}
        onPathChange={setPath} onSearchChange={setSearchValue} onViewModeChange={setView} onSelect={onSelect}
        canOpen={(entry) => entry.actions?.includes("OPEN")}
        canSelect={(entry) => entry.actions?.includes("SELECT") && entry.name.toLowerCase().endsWith(".zip")}
        formatSize={(size) => `${(size / 1024).toFixed(1)} KiB`}
    />;
}
