import {FunctionComponent} from "react";
import {ImporterInfoResponse} from "./index";
import ImporterIndex from "./components/ImporterIndex";

export type AppBaseProps = {
    pluginInfo: ImporterInfoResponse;
}

const AppBase: FunctionComponent<AppBaseProps> = ({pluginInfo}) => {
    return <ImporterIndex data={pluginInfo}/>;
}

export default AppBase;
