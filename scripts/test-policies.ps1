param(
    [string]$GatewayUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

$policy = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/policies" -ContentType 'application/json' -Body (@{
    souscripteurEmail = 'insured@example.com'
    typeCouverture = 'AUTO'
    montantCouvert = 50000.00
    prime = 600.00
    dateEffet = '2026-01-01'
    statut = 'ACTIVE'
} | ConvertTo-Json)

Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/policies/$($policy.id)/guarantees" -ContentType 'application/json' -Body (@{
    libelle = 'COLLISION'
    plafond = 15000.00
    franchise = 500.00
} | ConvertTo-Json) | Out-Null

$coverage = Invoke-RestMethod -Method Get -Uri "$GatewayUrl/api/policies/$($policy.id)/coverage?claimType=COLLISION"

if (-not $coverage.covered) {
    throw "Expected a covered claim, but received: $($coverage | ConvertTo-Json -Compress)"
}

Write-Host "Smoke test passed. Policy $($policy.id) is covered up to $($coverage.plafond) with a franchise of $($coverage.franchise)."
