package com.saas.system.domain.port.out;

import com.saas.system.domain.model.Constant;

public interface IEventPublisherPort {
    void publishConstantUpdated(Constant constant);
}
