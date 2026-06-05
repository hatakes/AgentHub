package com.sean.agenthub.agent.core.tool;

import com.sean.agenthub.agent.core.api.*;
import com.sean.agenthub.agent.core.model.*;
import com.sean.agenthub.agent.core.tool.*;

/**
 * Tool 风险等级，MVP 阶段只开放 READ。
 *
 * @author Sean
 */
public enum ToolRiskLevel {
    READ,
    WRITE,
    DANGEROUS
}
