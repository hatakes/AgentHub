package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * Tool 风险等级，MVP 阶段只开放 READ。
 *
 * <p>Runtime 在执行前校验 Tool 的风险等级，MVP 只允许 READ 级别的 Tool。
 * 这个限制放在 Runtime 层，防止业务侧误注册高风险 Tool 后被模型直接触发。</p>
 *
 * @author Sean
 */
public enum ToolRiskLevel {
    /** 只读操作，如查询、检索、分析。MVP 只允许此级别。 */
    READ,
    /** 写入操作，如创建、更新、删除。后续阶段开放。 */
    WRITE,
    /** 危险操作，如执行代码、发送消息。需要额外审批流程。 */
    DANGEROUS
}
