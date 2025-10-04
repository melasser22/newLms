import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Tenant dashboard happy path"
    request {
        method 'GET'
        url '/api/bff/tenants/42/dashboard'
        headers {
            header 'Authorization', consumer(regex('Bearer .+'))
            header 'X-Tenant-Id', value(consumer(regex('.+')), producer('integration-tenant'))
        }
    }
    response {
        status OK()
        headers {
            header 'Content-Type', value(regex('application/json.*'))
        }
    }
}
